cs263 project
=====

Point Cloud processing on Google App Engine

There are relatively cheap 3d sensor now available to the consumer such as the Microsoft Kinect. Most of the point cloud processing is done on a PC with the PCL library. My project will offload the computation. I will begin with normal estimation algorithm and then continue with segmentation.

Point clouds will be uploaded using a rest API. The point cloud, normals and clusters will be stored in the blobstore.
Tasks queues will be used to initiate processing for each stage.


Future work may include object classification.
