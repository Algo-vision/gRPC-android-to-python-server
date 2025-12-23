import grpc
from concurrent import futures
import cv2
import numpy as np
import time

# Import the generated classes
# These imports assume you will generate the python code from the proto file
# and place it in the same directory or adjust PYTHONPATH
import side_camera_image_service_pb2
import side_camera_image_service_pb2_grpc
from google.protobuf import empty_pb2

class SideCameraImageService(side_camera_image_service_pb2_grpc.SideCameraImageServiceServicer):
    def SubmitSideCameraImage(self, request_iterator, context):
        print("Receiving stream of images...")
        try:
            for request in request_iterator:
                # Get the image data from bytes
                nparr = np.frombuffer(request.image_data, np.uint8)
                img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                # img = cv2.transpose(img)
                if img is not None:
                    # Log info
                    timestamp = request.timestamp if request.HasField('timestamp') else int(time.time() * 1000)
                    camera_id = request.camera_id if request.HasField('camera_id') else "unknown"
                    
                    print(f"Received frame: {img.shape} from {camera_id} at {timestamp}")

                    # Display the image
                    cv2.imshow(f'Side Camera Stream from steam {camera_id}', img)
                    
                    # Press 'q' to quit the window (though this runs in a server loop)
                    if cv2.waitKey(1) & 0xFF == ord('q'):
                        break
                else:
                    print("Failed to decode image")
                    
        except Exception as e:
            print(f"Error processing stream: {e}")
            
        cv2.destroyAllWindows()
        return empty_pb2.Empty()

    def ObservePredictions(self, request, context):
        # Implementation for observing predictions if needed
        # For now, just keep the stream open
        try:
            while True:
                time.sleep(1)
                # yield side_camera_image_service_pb2.ObservePredictionsResponse(...)
        except Exception as e:
            print(f"Error in ObservePredictions: {e}")

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    side_camera_image_service_pb2_grpc.add_SideCameraImageServiceServicer_to_server(
        SideCameraImageService(), server
    )
    
    # Listen on all interfaces on port 50051
    port = 50051
    server.add_insecure_port(f'[::]:{port}')
    print(f"Server started, listening on {port}")
    
    server.start()
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        print("Server stopping...")
        server.stop(0)

if __name__ == '__main__':
    serve()
