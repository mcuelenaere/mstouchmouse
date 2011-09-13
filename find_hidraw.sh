#!/bin/sh

ls /sys/bus/hid/devices/*:045E:0773.*/hidraw/ | grep -e "^hidraw[0-9]*$" | tail -n 1
