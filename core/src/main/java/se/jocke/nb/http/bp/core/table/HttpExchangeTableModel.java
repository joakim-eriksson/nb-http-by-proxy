package se.jocke.nb.http.bp.core.table;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toCollection;
import javax.swing.table.AbstractTableModel;
import se.jocke.nb.http.bp.core.HttpExchange;

public final class HttpExchangeTableModel extends AbstractTableModel {

    private TableModelImpl tableModel;

    public HttpExchangeTableModel(Predicate<HttpExchange> filter) {
        this.tableModel = new TableModelImpl(Collections.emptySet(), filter);
    }

    public void onEvent(HttpExchange event) {
        int rowCount = getRowCount();
        tableModel = tableModel.withEvent(event);
        int newRowCount = getRowCount();

        if (newRowCount > rowCount) {
            final int changedRow = getRowCount() - 1;
            fireTableRowsInserted(changedRow, changedRow);
        } else if (tableModel.isNotFiltered(event)) {
            int indexOfEvent = tableModel.indexOf(event);
            if (indexOfEvent >= 0) {
                fireTableRowsUpdated(indexOfEvent, indexOfEvent);
            }
        }
    }

    public void onFilterChange(Predicate<HttpExchange> predicate) {
        int rowCount = getRowCount();
        tableModel = tableModel.withFilter(predicate);
        int newRowCount = getRowCount();

        if (rowCount != newRowCount) {
            fireTableDataChanged();
        }
    }

    public void onClearFilter() {
        onFilterChange(TableModelImpl.alwaysFalse);
    }

    public void onClearEvents() {
        int rowCount = getRowCount();
        tableModel = tableModel.asEmpty();
        int newRowCount = getRowCount();

        if (rowCount != newRowCount) {
            fireTableDataChanged();
        }
    }

    @Override
    public int getRowCount() {
        return tableModel.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return tableModel.getColumnCount();
    }

    @Override
    public String getColumnName(int column) {
        return tableModel.getColumnName(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return tableModel.getValueAt(rowIndex, columnIndex);
    }

    public HttpExchange getExchangeAt(int rowIndex) {
        return tableModel.getExchangeAt(rowIndex);
    }

    private static final class TableModelImpl extends AbstractTableModel {

        public enum VisibleColumn {
            URI {
                @Override
                Object getValue(HttpExchange event) {
                    return event.getRequestURI();
                }
            },
            METHOD {
                @Override
                Object getValue(HttpExchange event) {
                    return event.getMethod();
                }
            },
            STATUS {
                @Override
                Object getValue(HttpExchange event) {
                    return event.getStatus()  > 0 ? event.getStatus() : "pending";
                }
            },
            DURATION {
                @Override
                Object getValue(HttpExchange event) {
                    return event.getDuration() > 0 ? (event.getDuration() + "ms") : "pending";
                }
            };

            abstract Object getValue(HttpExchange event);
        }

        private final NavigableSet<HttpExchange> events;

        private final NavigableSet<HttpExchange> filteredEvents;

        private static final Predicate<HttpExchange> alwaysFalse = (evt) -> false;

        private final Predicate<HttpExchange> filter;

        private final HttpExchange[] eventsArray;

        private final List<VisibleColumn> columns = List.of(VisibleColumn.values());

        public TableModelImpl(Collection<HttpExchange> events, Predicate<HttpExchange> filter) {
            this.events = new TreeSet<>(events);
            this.filteredEvents = this.events.stream().filter(filter).collect(toCollection(TreeSet::new));
            this.filter = filter;
            this.eventsArray = eventsAsArray();
        }

        public TableModelImpl(Collection<HttpExchange> events, Predicate<HttpExchange> filter, HttpExchange event) {
            this.events = new TreeSet<>(events);
            this.events.remove(event);
            this.events.add(event);
            this.filter = filter;
            this.filteredEvents = this.events.stream().filter(filter).collect(toCollection(TreeSet::new));
            this.eventsArray = eventsAsArray();
        }

        public boolean isNotFiltered(HttpExchange event) {
            return filter.test(event);
        }

        @Override
        public int getRowCount() {
            return getDisplayedEvents().size();
        }

        public int indexOf(HttpExchange event) {
            return Arrays.binarySearch(eventsArray, event);
        }

        private HttpExchange[] eventsAsArray() {
            return filteredEvents.toArray(new HttpExchange[0]);
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns.get(column).name();
        }

        public TableModelImpl withEvent(HttpExchange event) {
            return new TableModelImpl(events, filter, event);
        }

        public TableModelImpl withFilter(Predicate<HttpExchange> filter) {
            return new TableModelImpl(events, filter);
        }

        public TableModelImpl asEmpty() {
            return new TableModelImpl(Collections.emptySet(), filter);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns.get(columnIndex).getValue(eventsArray[rowIndex]);
        }
        
        public HttpExchange getExchangeAt(int rowIndex) {
            return eventsArray[rowIndex];
        }

        public NavigableSet<HttpExchange> getDisplayedEvents() {
            return filteredEvents;
        }
    }
}
