注意：以下命令需要用sudo权限运行

1.配置jdk环境变量
export JAVA_HOME=/home/jdk-19.0.2+7
export PATH=$JAVA_HOME/bin:$PATH


2.linux下面安装android sdk
apt install sdkmanager
local.properties配置sdk.dir=/opt/android-sdk
sdkmanager --install "build-tools;36.0.0-rc5"
sdkmanager --install "platforms;android-36"
sdkmanager --install "ndk;27.0.12077973"
sdkmanager --install "cmake;3.22.1"

如果不需要在linux下面打包android apk可以将以下代码注释
compose/build.gradle.kts
// alias(libs.plugins.androidApplication)

/*   androidTarget {
compilerOptions {
jvmTarget.set(JvmTarget.JVM_21)
}
}*/

/*   androidMain.dependencies {
implementation(compose.preview)
implementation(libs.androidx.activity.compose)
compileOnly(files("libs/framework.jar"))
}*/

/*
android {
}
dependencies {
debugImplementation(compose.uiTooling)
}
*/*/

build.gradle.kts
//    alias(libs.plugins.androidApplication) apply false
//    alias(libs.plugins.androidLibrary) apply false




3.linux debian需要安装以下才能正常打包deb包 
apt install fakeroot

4.linux打包可执行文件的常用命令
sh gradlew run 执行运行GUI代码
sh gradlew createDistributable 打包可以执行文件
sh gradlew createReleaseDistributable
sh gradlew packageDeb  打包可分发的deb包
sh gradlew packageDistributionForCurrentOS


5.如果需要编译jni目录下的socketcan.cpp
请注意Makefile中的JAVA_HOME路径是否正确
在jni目录下运行make all即可编译对应平台的so包

6.linux debian安装好deb包可以用sudo命令运行
sudo /opt/socketcan/bin/SocketCan
