#!/bin/bash

RUN_CMD="java -jar freshAirController-*.jar"

if [ "$user" ]; then
   RUN_CMD="$RUN_CMD --user $user"
else
   echo "user is not exist."
   exit 1
fi

if [ "$password" ]; then
   RUN_CMD="$RUN_CMD --password $password"
else
   echo "password is not exist."
   exit 1
fi

if [ "${freshAirSN}" ]; then
  RUN_CMD="$RUN_CMD --fresh-air-sn ${freshAirSN}"
else
  echo "fresh-air-sn is not exist"
  exit 1
fi

if [ "${powerSN}" ]; then
  RUN_CMD="$RUN_CMD --power-sn ${powerSN}"
else
  echo "power-sn is not exist"
  exit 1
fi

if [ "${powerLimit}" ]; then
  RUN_CMD="$RUN_CMD --power-limit ${powerLimit}"
fi

if [ "${freshaiAliveDelay}" ]; then
  RUN_CMD="$RUN_CMD --freshair-alive-delay ${freshaiAliveDelay}"
fi


echo $RUN_CMD
eval $RUN_CMD