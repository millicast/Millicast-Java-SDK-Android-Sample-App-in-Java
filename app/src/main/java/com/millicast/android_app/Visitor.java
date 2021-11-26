package com.millicast.android_app;

import com.millicast.StatsLeaf;
import com.millicast.StatsNode;
import com.millicast.StatsTree;

class Visitor implements StatsTree.Visitor {
    private final StringBuilder stringBuilder;

    public Visitor() {
        this.stringBuilder = new StringBuilder();
    }

    @Override
    public void process(StatsNode statsNode) {
        stringBuilder.append("{ node : ");
        stringBuilder.append(statsNode.getName());
    }

    @Override
    public void process(StatsLeaf statsLeaf) {
        stringBuilder.append(", name : ");
        stringBuilder.append(statsLeaf.getName());
        stringBuilder.append(", value : ");
        stringBuilder.append(statsLeaf.getValue());
        stringBuilder.append('}');
    }

    public String toString() {
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}

