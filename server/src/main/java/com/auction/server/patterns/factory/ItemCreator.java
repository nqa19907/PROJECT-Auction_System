package com.auction.server.patterns.factory;
import com.auction.shared.model.Item;

public interface ItemCreator {
    public Item createItem();
}
