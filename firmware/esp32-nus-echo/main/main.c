/*
 * BLE test peripheral for BluetoothTerminal.
 *
 * Exposes the Nordic UART Service (NUS):
 *   service 6e400001-b5a3-f393-e0a9-e50e24dcca9e
 *   RX      6e400002-...  app -> ESP32 (write / write without response)
 *   TX      6e400003-...  ESP32 -> app (notify)
 *
 * Whatever the app writes to RX is logged and echoed back on TX.
 * While the app is subscribed to TX, a "tick N" line is also sent every 2 s.
 */
#include <stdio.h>
#include <string.h>

#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "nvs_flash.h"

#include "host/ble_hs.h"
#include "host/util/util.h"
#include "nimble/nimble_port.h"
#include "nimble/nimble_port_freertos.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"

#define DEVICE_NAME "ESP32-NUS"

static const char *TAG = "nus";

/* NimBLE stores 128-bit UUIDs little-endian, so the bytes are reversed. */
static ble_uuid128_t nus_svc_uuid = BLE_UUID128_INIT(
    0x9e, 0xca, 0xdc, 0x24, 0x0e, 0xe5, 0xa9, 0xe0, 0x93, 0xf3, 0xa3, 0xb5, 0x01, 0x00, 0x40, 0x6e);
static ble_uuid128_t nus_rx_uuid = BLE_UUID128_INIT(
    0x9e, 0xca, 0xdc, 0x24, 0x0e, 0xe5, 0xa9, 0xe0, 0x93, 0xf3, 0xa3, 0xb5, 0x02, 0x00, 0x40, 0x6e);
static ble_uuid128_t nus_tx_uuid = BLE_UUID128_INIT(
    0x9e, 0xca, 0xdc, 0x24, 0x0e, 0xe5, 0xa9, 0xe0, 0x93, 0xf3, 0xa3, 0xb5, 0x03, 0x00, 0x40, 0x6e);

static uint16_t tx_val_handle;
static volatile uint16_t conn_handle = BLE_HS_CONN_HANDLE_NONE;
static volatile bool notify_enabled;
static uint8_t own_addr_type;

static void advertise(void);

static void nus_send(const void *data, uint16_t len)
{
    if (conn_handle == BLE_HS_CONN_HANDLE_NONE || !notify_enabled) {
        return;
    }
    struct os_mbuf *om = ble_hs_mbuf_from_flat(data, len);
    if (om == NULL) {
        ESP_LOGW(TAG, "out of mbufs");
        return;
    }
    /* Takes ownership of om, even on failure. */
    int rc = ble_gatts_notify_custom(conn_handle, tx_val_handle, om);
    if (rc != 0) {
        ESP_LOGW(TAG, "notify failed: %d", rc);
    }
}

static int nus_access(uint16_t conn, uint16_t attr_handle, struct ble_gatt_access_ctxt *ctxt, void *arg)
{
    /* Only called from the NimBLE host task, so a static buffer is safe. */
    static uint8_t buf[BLE_ATT_ATTR_MAX_LEN];

    if (ctxt->op != BLE_GATT_ACCESS_OP_WRITE_CHR) {
        return BLE_ATT_ERR_REQ_NOT_SUPPORTED;
    }
    uint16_t len = 0;
    if (ble_hs_mbuf_to_flat(ctxt->om, buf, sizeof(buf), &len) != 0) {
        return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
    }
    ESP_LOGI(TAG, "RX %u bytes: %.*s", len, len, (const char *)buf);
    ESP_LOG_BUFFER_HEX_LEVEL(TAG, buf, len, ESP_LOG_DEBUG);

    /* Echo back unchanged: a write is at most MTU - 3 bytes, so it always fits one notification. */
    nus_send(buf, len);
    return 0;
}

static const struct ble_gatt_svc_def gatt_svcs[] = {
    {
        .type = BLE_GATT_SVC_TYPE_PRIMARY,
        .uuid = &nus_svc_uuid.u,
        .characteristics = (struct ble_gatt_chr_def[]){
            {
                .uuid = &nus_rx_uuid.u,
                .access_cb = nus_access,
                .flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_NO_RSP,
            },
            {
                .uuid = &nus_tx_uuid.u,
                .access_cb = nus_access,
                .val_handle = &tx_val_handle,
                .flags = BLE_GATT_CHR_F_NOTIFY,
            },
            {0},
        },
    },
    {0},
};

static int gap_event(struct ble_gap_event *event, void *arg)
{
    switch (event->type) {
    case BLE_GAP_EVENT_CONNECT:
        if (event->connect.status == 0) {
            conn_handle = event->connect.conn_handle;
            ESP_LOGI(TAG, "connected (handle %d)", conn_handle);
        } else {
            ESP_LOGW(TAG, "connect failed: %d", event->connect.status);
            advertise();
        }
        return 0;

    case BLE_GAP_EVENT_DISCONNECT:
        ESP_LOGI(TAG, "disconnected (reason 0x%x)", event->disconnect.reason);
        conn_handle = BLE_HS_CONN_HANDLE_NONE;
        notify_enabled = false;
        advertise();
        return 0;

    case BLE_GAP_EVENT_ADV_COMPLETE:
        advertise();
        return 0;

    case BLE_GAP_EVENT_SUBSCRIBE:
        if (event->subscribe.attr_handle == tx_val_handle) {
            notify_enabled = event->subscribe.cur_notify;
            ESP_LOGI(TAG, "TX notifications %s", notify_enabled ? "on" : "off");
        }
        return 0;

    case BLE_GAP_EVENT_MTU:
        ESP_LOGI(TAG, "MTU = %d (max write %d bytes)", event->mtu.value, event->mtu.value - 3);
        return 0;

    default:
        return 0;
    }
}

static void advertise(void)
{
    /* Flags (3) + 128-bit UUID (18) leave no room for the name, so the name goes in the scan response. */
    struct ble_hs_adv_fields fields = {0};
    fields.flags = BLE_HS_ADV_F_DISC_GEN | BLE_HS_ADV_F_BREDR_UNSUP;
    fields.uuids128 = &nus_svc_uuid;
    fields.num_uuids128 = 1;
    fields.uuids128_is_complete = 1;
    int rc = ble_gap_adv_set_fields(&fields);
    if (rc != 0) {
        ESP_LOGE(TAG, "adv fields: %d", rc);
        return;
    }

    struct ble_hs_adv_fields rsp = {0};
    const char *name = ble_svc_gap_device_name();
    rsp.name = (uint8_t *)name;
    rsp.name_len = strlen(name);
    rsp.name_is_complete = 1;
    rc = ble_gap_adv_rsp_set_fields(&rsp);
    if (rc != 0) {
        ESP_LOGE(TAG, "scan rsp fields: %d", rc);
        return;
    }

    struct ble_gap_adv_params params = {0};
    params.conn_mode = BLE_GAP_CONN_MODE_UND;
    params.disc_mode = BLE_GAP_DISC_MODE_GEN;
    rc = ble_gap_adv_start(own_addr_type, NULL, BLE_HS_FOREVER, &params, gap_event, NULL);
    if (rc != 0) {
        ESP_LOGE(TAG, "adv start: %d", rc);
        return;
    }
    ESP_LOGI(TAG, "advertising as \"%s\"", name);
}

static void on_sync(void)
{
    ble_hs_util_ensure_addr(0);
    ble_hs_id_infer_auto(0, &own_addr_type);
    advertise();
}

static void on_reset(int reason)
{
    ESP_LOGE(TAG, "host reset: %d", reason);
}

static void host_task(void *param)
{
    nimble_port_run(); /* returns only after nimble_port_stop() */
    nimble_port_freertos_deinit();
}

static void tick_task(void *param)
{
    uint32_t n = 0;
    char msg[32];
    for (;;) {
        vTaskDelay(pdMS_TO_TICKS(2000));
        if (notify_enabled) {
            int len = snprintf(msg, sizeof(msg), "tick %lu\r\n", (unsigned long)++n);
            nus_send(msg, len);
        }
    }
}

void app_main(void)
{
    esp_err_t ret = nvs_flash_init(); /* the BLE controller keeps calibration data in NVS */
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    ESP_ERROR_CHECK(nimble_port_init());
    ble_hs_cfg.sync_cb = on_sync;
    ble_hs_cfg.reset_cb = on_reset;

    ble_svc_gap_init();
    ble_svc_gatt_init();
    ESP_ERROR_CHECK(ble_svc_gap_device_name_set(DEVICE_NAME));
    ESP_ERROR_CHECK(ble_gatts_count_cfg(gatt_svcs));
    ESP_ERROR_CHECK(ble_gatts_add_svcs(gatt_svcs));

    xTaskCreate(tick_task, "tick", 3072, NULL, 5, NULL);
    nimble_port_freertos_init(host_task);
}
