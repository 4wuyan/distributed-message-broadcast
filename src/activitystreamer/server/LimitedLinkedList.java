package activitystreamer.server;

import java.util.LinkedList;

public class LimitedLinkedList<E> extends LinkedList<E> {
    private int limit;

    LimitedLinkedList(int limit) {
        super();
        if (limit > 0) {
            this.limit = limit;
        }
        else {
            this.limit = 1;
        }
    }

    @Override
    public boolean add(E o) {
        boolean answer = super.add(o);
        while (size() > limit) {
            super.remove();
        }
        return answer;
    }
}
