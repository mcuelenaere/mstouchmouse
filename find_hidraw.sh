#!/bin/sh

RDESC=`grep -l ff00.fa0a /sys/kernel/debug/hid/*\:045E\:0773.*/rdesc`
NAME=`echo $RDESC | sed "s/.*\/\(.*:.*:.*\.[^\/]*\).*/\1/"`
HIDRAW=`ls /sys/bus/hid/devices/$NAME/hidraw/ | grep -e "^hidraw[0-9]*$"`

echo /dev/$HIDRAW
