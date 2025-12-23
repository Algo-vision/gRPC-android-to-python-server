# Android gRPC Sample App

A minimal Android sample application demonstrating integration with the `SideCameraImageService`
gRPC service. This project is designed as a reference implementation for external vendors to
integrate camera image streaming with gRPC.

## Project Structure

```
sample-android-grpc/
├── app/              # Main application with Compose UI
├── grpc/             # gRPC client implementation
├── image-stream/     # Image stream abstraction layer
└── python_server/    # Python gRPC server implementation
```

## Getting Started

### 1. Python Server Setup

The Python server receives images from the Android app. It uses `gRPC` and `OpenCV` to display the frames.

#### Prerequisites
- Python 3.8+
- pip

#### Installation & Running

1. Navigate to the python server directory:
   ```bash
   cd python_server
   ```

2. Generate Proto files and install requirements:
   The `generate_protos.sh` script handles virtual environment creation, dependency installation, and proto code generation.
   ```bash
   chmod +x generate_protos.sh
   ./generate_protos.sh
   ```

3. Run the server:
   ```bash
   # Make sure the virtual environment is activated if not already
   source venv/bin/activate
   
   python server.py
   ```
   The server will start listening on port `50051`.

### 2. Android App Setup

#### Prerequisites
- Android Studio Ladybug or newer
- Java 21

#### Configuration

1. Open `app/src/main/java/com/instacart/sample/di/AppModule.kt`.
2. Configure the `GrpcConfig`:
   ```kotlin
   // For Emulator (maps to localhost of host machine)
   single { GrpcConfig(host = "10.0.2.2", port = 50051, useTls = false) }
   
   // For Physical Device
   // Replace with your computer's local IP address (e.g., 192.168.1.x)
   // single { GrpcConfig(host = "192.168.1.5", port = 50051, useTls = false) }
   ```

#### Running the App

1. Build and install the app:
   ```bash
   ./gradlew installDebug
   ```
   Or use the **Run** button in Android Studio.

2. Grant Camera permissions when prompted.
3. The app will automatically attempt to connect to the configured gRPC server and start streaming camera frames.
4. Verify the connection in the app UI logs and see the video stream on your computer screen (if running the Python server).

## Architecture

### Modules

- **app**: Main application module with Jetpack Compose UI showing service status and logs
- **grpc**: Contains the proto definitions and a basic gRPC client implementation for
  `SideCameraImageService`
- **image-stream**: Provides image stream abstraction via `ImageStream` interface
- **python_server**: A simple Python server that implements the gRPC service for testing

## Troubleshooting

### Connection Issues

- **Emulator**: Ensure you use `10.0.2.2` as the host IP.
- **Physical Device**: Ensure both the phone and computer are on the same Wi-Fi network. Check firewall settings on your computer to allow incoming connections on port 50051.

### Proto Generation Issues

- Run `./gradlew :grpc:clean :grpc:build` to regenerate Android bindings.
- Check `grpc/build/generated/source/proto/` for generated files.

## License

This is a sample project for integration reference.
