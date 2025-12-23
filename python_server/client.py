import grpc
import cv2
import time
import sys
import os

# Import the generated classes
import side_camera_image_service_pb2
import side_camera_image_service_pb2_grpc

def generate_frames(video_path):
    cap = cv2.VideoCapture(video_path)
    
    if not cap.isOpened():
        print(f"Error: Could not open video file {video_path}")
        return

    camera_id = "python_client_video"
    print(f"Streaming video from {video_path}...")

    try:
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                print("End of video stream.")
                break

            # Encode frame to JPEG
            # The server uses cv2.imdecode, so we need to send encoded image data
            ret, buffer = cv2.imencode('.jpg', frame)
            
            if not ret:
                continue
                
            image_bytes = buffer.tobytes()
            timestamp = int(time.time() * 1000)

            yield side_camera_image_service_pb2.SubmitCameraFrameRequest(
                image_data=image_bytes,
                camera_id=camera_id,
                timestamp=timestamp
            )
            
            # Simulate 30 FPS
            time.sleep(1/30)
            
    finally:
        cap.release()

def run():
    if len(sys.argv) < 2:
        print("Usage: python client.py <path_to_video_file>")
        print("Example: python client.py my_video.mp4")
        # Fallback to webcam if no file provided? Or just exit.
        # Let's try to open webcam 0 if no file is provided for convenience
        video_source = 0
        print("No video file provided, attempting to use Webcam 0")
    else:
        video_source = sys.argv[1]
        if not os.path.exists(video_source):
             print(f"File not found: {video_source}")
             return

    # Create gRPC channel
    channel = grpc.insecure_channel('localhost:50051')
    stub = side_camera_image_service_pb2_grpc.SideCameraImageServiceStub(channel)

    print("Connecting to gRPC server at localhost:50051...")
    
    try:
        # SubmitSideCameraImage expects a stream of requests
        response = stub.SubmitSideCameraImage(generate_frames(video_source))
        print("Stream finished.")
    except grpc.RpcError as e:
        print(f"gRPC Error: {e}")
    except KeyboardInterrupt:
        print("Stopping client...")

if __name__ == '__main__':
    run()
