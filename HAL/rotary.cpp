#include "../include/rotary.h"
#include <iostream>
#include <thread>
#include <chrono>

RotaryEncoder::RotaryEncoder(int gpioA, int gpioB, int gpioSW)
    : pinA(gpioA), pinB(gpioB), pinSW(gpioSW), counter(5), lastStateA(false) {}

bool RotaryEncoder::initialize() {
    // Export and set GPIO directions
    if (!gpio.exportGpio(pinA) || !gpio.exportGpio(pinB)) {
        std::cerr << "Failed to export GPIO pins!" << std::endl;
        return false;
    }

    if (!gpio.setGpioDirection(pinA, "in") || !gpio.setGpioDirection(pinB, "in")) {
        std::cerr << "Failed to set GPIO directions!" << std::endl;
        return false;
    }

    if (pinSW != -1 && (!gpio.exportGpio(pinSW) || !gpio.setGpioDirection(pinSW, "in"))) {
        std::cerr << "Failed to initialize button GPIO!" << std::endl;
        return false;
    }

    // Read initial state of Channel A
    if (!gpio.getGpioValue(pinA, &lastStateA)) {
        std::cerr << "Failed to read initial state of GPIO A!" << std::endl;
        return false;
    }

    return true;
}

int RotaryEncoder::update() {
    bool stateA, stateB;
    if (gpio.getGpioValue(pinA, &stateA) && gpio.getGpioValue(pinB, &stateB)) {
            if(stateA != lastStateA) {
                if (stateB != stateA) {
                 counter++; // Clockwise
                std::cout << "Clockwise: Counter = " << counter << std::endl;
            } else {
                counter--; // Counterclockwise
                std::cout << "Counterclockwise: Counter = " << counter << std::endl;
            }
            lastStateA = stateA;
            } else {
                counter = 0;
            }
        }
    return counter;
}

int RotaryEncoder::getCounter() const {
    return counter;
}

void RotaryEncoder::resetCounter() {
    counter = 0;
}

bool RotaryEncoder::isButtonPressed(bool &pressed) {
    if (pinSW != -1) {
        return gpio.getGpioValue(pinSW, &pressed);
    }
    return false;
}

