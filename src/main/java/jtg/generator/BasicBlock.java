package jtg.generator;

import soot.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class BasicBlock {
    private final List<Unit> units;
    public BasicBlock(List<Unit> units) {
        this.units = new ArrayList<>(units);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Unit unit : units) {
            sb.append(unit).append('\n');
        }
        return sb.toString();
    }
    public Unit getHead(){
        checkSize();
        return units.get(0);
    }
    private void checkSize(){
        if (units.isEmpty()) throw new NoSuchElementException();
    }
    public Unit getTail(){
        checkSize();
        return units.get(units.size() - 1);
    }
}
