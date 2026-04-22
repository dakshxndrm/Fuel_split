package com.example.fuel_split;

import java.io.Serializable;
import java.util.Map;

public class Trip implements Serializable {
    private String tripName, distance, fuel, total, members, paidBy;
    private Map<String, Double> allShares;

    public Trip(String tripName, String distance, String fuel, String total,
                String members, Map<String, Double> allShares, String paidBy) {
        this.tripName = tripName;
        this.distance = distance;
        this.fuel = fuel;
        this.total = total;
        this.members = members;
        this.allShares = allShares;
        this.paidBy = paidBy;
    }

    public String getTripName()              { return tripName; }
    public String getDistance()              { return distance; }
    public String getFuel()                  { return fuel; }
    public String getTotal()                 { return total; }
    public String getMembers()               { return members; }
    public Map<String, Double> getAllShares() { return allShares; }
    public String getPaidBy()                { return paidBy; }
}