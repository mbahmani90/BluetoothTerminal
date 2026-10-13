# ESP32 NUS echo

ESP-IDF firmware (NimBLE) that turns an ESP32 into a BLE test device for BluetoothTerminal.

It advertises as `ESP32-NUS` with the Nordic UART Service:

| | UUID | Direction |
|---|---|---|
| Service | `6e400001-b5a3-f393-e0a9-e50e24dcca9e` | |
| RX | `6e400002-b5a3-f393-e0a9-e50e24dcca9e` | app → ESP32 (write, write without response) |
| TX | `6e400003-b5a3-f393-e0a9-e50e24dcca9e` | ESP32 → app (notify) |

- Every write to RX is logged and echoed back on TX.
- While the app is subscribed to TX, `tick N` is sent every 2 s.
- The preferred MTU is 517, matching the app's `requestMtu(517)`.
- Advertising stops while connected and restarts after a disconnect.

## Build and flash

Builds with ESP-IDF v5.5 and v6.0.

```bash
. $IDF_PATH/export.sh
idf.py set-target esp32          # or esp32s3, esp32c3, ...
idf.py -p /dev/cu.usbserial-XXXX flash monitor
```

The monitor logs connect, MTU, subscribe, received data and disconnect events.
