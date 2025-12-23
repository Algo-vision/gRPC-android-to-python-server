# Python gRPC Server for Side Camera

This is a Python implementation of the `SideCameraImageService` server.
It receives image frames via gRPC and displays them using OpenCV.

## Setup

1.  Make sure you have Python 3 installed.
2.  Run the setup script to create a virtual environment, install dependencies, and generate the gRPC code:

    ```bash
    chmod +x generate_protos.sh
    ./generate_protos.sh
    ```

## Running the Server

1.  Activate the virtual environment (if not already activated):

    ```bash
    source venv/bin/activate
    ```

2.  Run the server:

    ```bash
    python server.py
    ```

The server will listen on port 50051. Press 'q' in the OpenCV window to close the display (note: this might not stop the server completely, use Ctrl+C in the terminal to stop the server).
