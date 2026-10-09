package com.diodeit.spendtrack.models;

public interface ListItem {
    int TYPE_HEADER = 0;
    int TYPE_TRANSACTION = 1;

    int getItemType();
}