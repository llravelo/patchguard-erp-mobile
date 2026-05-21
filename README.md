# PatchGuard

Road-surface capture system. An iOS app samples camera frames at a configurable rate, tags each frame with GPS metadata, and batch-uploads JPEGs to an ingest server.

## Components

| Component | Path | Description |
|-----------|------|-------------|
| iOS app | `PatchGuard/` | SwiftUI app (iOS 26.0+) |
| Test server | `server/` | Local Express.js mock for development |

---

## Requirements

- **iOS app**: Xcode 26+, physical iPhone or iPad (camera + GPS required), Apple Developer account for signing
- **Test server**: Node.js 18+

---

## Configuration

App configuration lives in `PatchGuard/PatchGuard/Info.plist`.

| Key | Default | Description |
|-----|---------|-------------|
| `SERVER_BASE_URL` | `https://api-patchguard.ngrok.dev` | Base URL of the production backend. |
| `MOCK_SERVER_BASE_URL` | `http://192.168.0.21:3000` | Base URL of the local test server. Update to your Mac's LAN IP. |
| `TEST_MODE` | `false` | When `true`, targets `MOCK_SERVER_BASE_URL` and skips authentication. |
| `BATCH_SIZE` | `10` | Number of frames accumulated before a POST is triggered. |

The app uses `NSAllowsArbitraryLoads` so plain HTTP to local addresses works without additional ATS configuration.

---

## Operating Modes

### Production mode (`TEST_MODE = false`)

- Targets `SERVER_BASE_URL`
- Shows a login screen on first launch; credentials are stored in Keychain and reused automatically
- Each batch POST includes a `Bearer` token obtained from `POST /api/v1/auth/login`
- After a successful batch upload, fires `POST /api/v1/analysis/trigger` to kick off server-side processing
- Expects HTTP 201 from the batch endpoint

### Test mode (`TEST_MODE = true`)

- Targets `MOCK_SERVER_BASE_URL`
- Login screen is bypassed entirely — no authentication
- Batch POST sends no `Authorization` header
- Expects HTTP 200 from the batch endpoint
- Use with the local Express server in `server/`

---

## Running the Test Server

```bash
cd server
npm install
npm start
```

The server binds to `0.0.0.0:3000` and is reachable from any device on the same network. Before starting, set `TEST_MODE` to `true` in `Info.plist` and update `MOCK_SERVER_BASE_URL` with your Mac's LAN IP:

```bash
ipconfig getifaddr en0   # find your Mac's LAN IP
```

```
GET  http://<mac-ip>:3000/health                  # health check
POST http://<mac-ip>:3000/api/v1/images/batch     # batch ingest (returns 200)
```

Uploaded JPEGs are saved to `server/uploads/`. Each batch is logged to stdout with GPS coordinates, altitude, heading, and accuracy.

---

## Running the iOS App

```bash
open PatchGuard/PatchGuard.xcodeproj
```

Build and run on a connected device from Xcode. CLI build (requires valid signing identity):

```bash
xcodebuild -project PatchGuard/PatchGuard.xcodeproj \
           -scheme PatchGuard \
           -destination 'generic/platform=iOS' \
           build
```

---

## Basic Usage

### Against the test server

1. Set `TEST_MODE = true` and `MOCK_SERVER_BASE_URL = http://<mac-ip>:3000` in `Info.plist`
2. Start the test server: `cd server && npm start`
3. Build and run the app on a device connected to the same Wi-Fi
4. The app goes straight to the capture screen — no login
5. Select a capture rate (1, 2, or 5 FPS) and tap **Start**
6. Batches upload automatically; check `server/uploads/` for saved frames

### Against production

1. Ensure `TEST_MODE = false` in `Info.plist`
2. Build and run the app
3. Enter your credentials on the login screen; they are saved to Keychain for future launches
4. Select a capture rate and tap **Start**

---

## Wire Format

`POST /api/v1/images/batch` — `multipart/form-data`

| Part | Type | Description |
|------|------|-------------|
| `files[]` | binary | JPEG image parts, one per frame |
| `items_json` | string | JSON array of metadata objects |

Each metadata object:

```json
{
  "filename": "frame_001.jpg",
  "latitude": 37.7749,
  "longitude": -122.4194,
  "captured_at": "2026-05-18T10:30:00Z",
  "heading": 270.0,
  "altitude": 15.3,
  "gps_accuracy": 4.1
}
```

`heading`, `altitude`, and `gps_accuracy` are optional.

The production endpoint returns HTTP 201 on success. The test server returns HTTP 200.
