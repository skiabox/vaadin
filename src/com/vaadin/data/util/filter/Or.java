package com.vaadin.data.util.filter;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;

/**
 * A compound {@link Filter} that accepts an item if any of its filters accept
 * the item.
 * 
 * If no filters are given, the filter should reject all items.
 * 
 * This filter also directly supports in-memory filtering when all sub-filters
 * do so.
 * 
 * @see And
 * 
 * @since 6.6
 */
public class Or extends AbstractJunctionFilter implements Filter {

    /**
     * 
     * @param filters
     *            filters of which the Or filter will be composed
     */
    public Or(Filter... filters) {
        super(filters);
    }

    public boolean passesFilter(Item item) throws UnsupportedFilterException {
        for (Filter filter : getFilters()) {
            if (filter.passesFilter(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if a change in the named property may affect the filtering
     * result. If some of the sub-filters are not in-memory filters, true is
     * returned.
     * 
     * By default, all sub-filters are iterated to check if any of them applies.
     * If there are no sub-filters, true is returned as an empty Or rejects all
     * items.
     */
    @Override
    public boolean appliesToProperty(Object propertyId) {
        if (getFilters().isEmpty()) {
            // empty Or filters out everything
            return true;
        } else {
            return super.appliesToProperty(propertyId);
        }
    }

}