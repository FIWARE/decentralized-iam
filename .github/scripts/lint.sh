#! /bin/bash

CHARTS=./charts/*
for chart in $CHARTS
do
 docker run --rm -v $(pwd):/apps alpine/helm:3 lint $chart
done
