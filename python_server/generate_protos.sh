#!/bin/bash

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    python3 -m venv venv
fi

# Activate virtual environment
source venv/bin/activate

# Install requirements
pip install -r requirements.txt

# Generate Python gRPC code
python3 -m grpc_tools.protoc -I../grpc/src/main/proto --python_out=. --grpc_python_out=. ../grpc/src/main/proto/side_camera_image_service.proto

echo "Proto files generated."
