#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <linux/can.h>
#include <linux/can/raw.h>

// 定义常量
#define CAN_FRAME_SIZE 12
#define CAN_MAX_DATA_LENGTH 8

// 错误码定义
#define SOCKET_OPEN_ERROR -1
#define SOCKET_BIND_ERROR -2
#define SUCCESS 0

extern "C" {

JNIEXPORT jint JNICALL
Java_com_example_socketcan_SocketCAN_socketcanOpen(JNIEnv *env, jobject thiz, jstring can_interface) {
    int socket_fd;
    struct ifreq interface_request;
    struct sockaddr_can address;

    // 创建CAN socket
    if ((socket_fd = socket(PF_CAN, SOCK_RAW, CAN_RAW)) < 0) {
        return SOCKET_OPEN_ERROR;
    }

    // 获取CAN接口名称
    const char *interface_name = env->GetStringUTFChars(can_interface, NULL);
    if (interface_name == NULL) {
        close(socket_fd);
        return SOCKET_OPEN_ERROR;
    }

    // 设置接口名称并获取索引
    strncpy(interface_request.ifr_name, interface_name, IFNAMSIZ - 1);
    interface_request.ifr_name[IFNAMSIZ - 1] = '\0';

    env->ReleaseStringUTFChars(can_interface, interface_name);

    if (ioctl(socket_fd, SIOCGIFINDEX, &interface_request) < 0) {
        close(socket_fd);
        return SOCKET_BIND_ERROR;
    }

    // 绑定socket到CAN接口
    memset(&address, 0, sizeof(address));
    address.can_family = AF_CAN;
    address.can_ifindex = interface_request.ifr_ifindex;

    if (bind(socket_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
        close(socket_fd);
        return SOCKET_BIND_ERROR;
    }

    return socket_fd;
}

JNIEXPORT jint JNICALL
Java_com_example_socketcan_SocketCAN_socketcanClose(JNIEnv *env, jobject thiz, jint socket_fd) {
    if (socket_fd >= 0) {
        return close(socket_fd);
    }
    return SUCCESS;
}

JNIEXPORT jint JNICALL
Java_com_example_socketcan_SocketCAN_socketcanWrite(JNIEnv *env, jobject thiz, jint socket_fd,
                                         jlong can_id, jboolean is_extended, jboolean is_remote,
                                         jint data_length, jbyteArray data_array) {
    if (socket_fd < 0 || data_length < 0 || data_length > CAN_MAX_DATA_LENGTH) {
        return -1;
    }

    struct can_frame frame;
    memset(&frame, 0, sizeof(frame));

    // 设置CAN ID和标志位
    frame.can_id = can_id & 0x1FFFFFFF;
    if (is_extended) {
        frame.can_id |= CAN_EFF_FLAG;
    }
    if (is_remote) {
        frame.can_id |= CAN_RTR_FLAG;
    }

    frame.can_dlc = data_length;

    // 复制数据
    if (data_length > 0) {
        jbyte *data_elements = env->GetByteArrayElements(data_array, NULL);
        if (data_elements == NULL) {
            return -1;
        }

        memcpy(frame.data, data_elements, data_length);
        env->ReleaseByteArrayElements(data_array, data_elements, JNI_ABORT);
    }

    return write(socket_fd, &frame, sizeof(frame));
}

JNIEXPORT jlongArray JNICALL
Java_com_example_socketcan_SocketCAN_socketcanRead(JNIEnv *env, jobject thiz, jint socket_fd) {
    if (socket_fd < 0) {
        return NULL;
    }

    struct can_frame frame;
    ssize_t bytes_read = read(socket_fd, &frame, sizeof(frame));

    if (bytes_read != sizeof(frame)) {
        return NULL;
    }

    // 创建返回数组：[can_id, is_extended, is_remote, data_length, data0, data1, ...]
    jlong result[CAN_FRAME_SIZE] = {0};

    result[0] = frame.can_id & 0x1FFFFFFF;  // CAN ID
    result[1] = (frame.can_id & CAN_EFF_FLAG) ? 1 : 0;  // 扩展帧标志
    result[2] = (frame.can_id & CAN_RTR_FLAG) ? 1 : 0;  // 远程帧标志
    result[3] = frame.can_dlc;  // 数据长度

    // 复制数据
    for (int i = 0; i < frame.can_dlc && i < CAN_MAX_DATA_LENGTH; i++) {
        result[i + 4] = frame.data[i];
    }

    jlongArray return_array = env->NewLongArray(CAN_FRAME_SIZE);
    if (return_array != NULL) {
        env->SetLongArrayRegion(return_array, 0, CAN_FRAME_SIZE, result);
    }

    return return_array;
}

} // extern "C"