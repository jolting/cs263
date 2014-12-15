cs263 project
=====

Point Cloud processing on Google App Engine

There are relatively cheap 3d sensor now available to the consumer such as the Microsoft Kinect. 
Most of the point cloud processing is done on a PC with the PCL library.
This is the my first attempt to do point cloud computations in the cloud.

Currently supports:
  Uploading point clouds via webpage.
  Listing point clouds.
  Programmatically uploading a point cloud from a kinect sensor.
 
Task queues are used to process PCD files.
Memcache is used to cache the point cloud list.
Datastore is used to store point cloud information(i.e. type, data format, resolution and name).
Blobstore is used to store the point data which consist of the points themselves are stored in the datastore.


Selenium tests
=====

You will have to modify the tests for the location of the pcd files. Relative paths don't seem to be supported.
Load the selenium tests suite in project/tests/

