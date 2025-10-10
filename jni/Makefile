# Compiler and flags
CXX := g++
CC := gcc
JAVA_HOME ?= /home/jdk-19.0.2+7
INCLUDES := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
CXXFLAGS := -fPIC -O2 -Wall -Wextra -std=c++11
CFLAGS := -fPIC -O2 -Wall -Wextra
LDFLAGS := -shared -Wl,-soname,libsocketcan.so

# Source files
SRC_CXX := socketcan.cpp
SRC_C := # Add C source files if any
OBJ := $(SRC_CXX:.cpp=.o) $(SRC_C:.c=.o)

# Target library
TARGET := libsocketcan.so

# Default target
all: $(TARGET)

# Compile C++ sources
%.o: %.cpp
	$(CXX) $(CXXFLAGS) $(INCLUDES) -c $< -o $@

# Compile C sources
%.o: %.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

# Link library
$(TARGET): $(OBJ)
	$(CXX) $(LDFLAGS) -o $@ $(OBJ)

# Install to system directory
install: $(TARGET)
	cp $(TARGET) /usr/lib/
	ldconfig

# Install to local directory
install-local: $(TARGET)
	mkdir -p ../lib
	cp $(TARGET) ../lib/

# Clean build files
clean:
	rm -f $(OBJ) $(TARGET)

# Debug build
debug: CXXFLAGS += -g -DDEBUG
debug: CFLAGS += -g -DDEBUG
debug: $(TARGET)

# Test compilation
test: $(TARGET)
	@echo "Build successful! Library: $(TARGET)"
	@file $(TARGET)

.PHONY: all clean install install-local debug test