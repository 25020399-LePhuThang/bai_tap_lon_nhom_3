package src.java.com.auction.shared.model;

import java.io.Serializable;

public interface Entity extends Serializable {
String getId();
void setId(String id);
}