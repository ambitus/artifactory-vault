#!/bin/bash

# Please fill <placholders> with the values that coorespond to the PostgreSQL versions you want to upgrade from and upgrade to.

docker build . --tag pg_upgrade:<INTEGER OLD VERSION NUMBER ONLY>-to-<INTEGER NEW VERSION NUMBER ONLY>
