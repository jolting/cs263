CFLAGS=`pkg-config --cflags pcl_io-1.7`
LDFLAGS=`pkg-config --libs pcl_io-1.7` -lboost_system

openni_uploader: openni_uploader.cpp
	g++ ${CFLAGS} openni_uploader.cpp ${LDFLAGS} -o openni_uploader
