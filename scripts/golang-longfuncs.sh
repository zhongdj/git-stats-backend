#!/usr/bin/env bash

cd $1 && golongfuncs -top 300 +lines +in_params +complexity +complexity/lines

