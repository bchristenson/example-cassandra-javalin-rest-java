package com.kineticdata.examples.javalin.daos;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.kineticdata.examples.javalin.models.Widget;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class WidgetDao {

    public static final String TABLE = "widgets";
    public static class Fields {
        public static final String TENANT_KEY = "tenant_key";
        public static final String KEY = "key";
        public static final String DESCRIPTION = "description";
    }
    
    /*----------------------------------------------------------------------------------------------
     * CONSTRUCTOR
     *--------------------------------------------------------------------------------------------*/
    
    private final Session session;
    private final ConcurrentHashMap<String,PreparedStatement> preparedStatements = new ConcurrentHashMap<>();
    
    public WidgetDao(Session session) {
        this.session = session;
    }
    
    /*----------------------------------------------------------------------------------------------
     * SERIALIZATION METHODS
     *--------------------------------------------------------------------------------------------*/
    
    public Widget toWidget(Row row) {
        return new Widget.Builder()
            .setTenantKey(row.getString(Fields.TENANT_KEY))
            .setKey(row.getString(Fields.KEY))
            .setDescription(row.getString(Fields.DESCRIPTION))
            .build();
    }
    
    /*----------------------------------------------------------------------------------------------
     * STATEMENT METHODS
     *--------------------------------------------------------------------------------------------*/
    
    protected BoundStatement bindListStatement(
        String tenantKey,
        Integer limit, 
        String offsetKey
    ) {
        // Obtain the prepared statement
        PreparedStatement preparedStatement = preparedStatements
            .computeIfAbsent("list", (key) -> {
                return session.prepare(
                    "SELECT * FROM "+TABLE
                    +" WHERE "+Fields.TENANT_KEY+" = :tenant_key"
                    +" AND "+Fields.KEY+" > :offset_key"
                    +" LIMIT :result_limit");
            });
        // Obtain a bound statement
        return preparedStatement.bind()
            .setString("tenant_key", tenantKey)
            .setString("offset_key", offsetKey)
            .setInt("result_limit", limit);
    }
    
    protected BoundStatement bindCreateStatement(
        Widget widget
    ) {
        // Obtain the prepared statement
        PreparedStatement preparedStatement = preparedStatements
            .computeIfAbsent("create", (key) -> {
                return session.prepare(
                    "INSERT INTO "+TABLE
                    +" ("
                        +Fields.TENANT_KEY+", "
                        +Fields.KEY+", "
                        +Fields.DESCRIPTION
                    +") VALUES ("
                        +":tenant_key, "
                        +":key, "
                        +":description"
                    +")"
                );
            });
        // Prepare the bound statement
        return preparedStatement.bind()
            .setString("tenant_key", widget.getTenantKey())
            .setString("key", widget.getKey())
            .setString("description", widget.getDescription());
    }
    
    protected BoundStatement bindRetrieveStatement(
        String tenantKey,
        String key
    ) {
        // Obtain the prepared statement
        PreparedStatement preparedStatement = preparedStatements
            .computeIfAbsent("retrieve", (name) -> {
                return session.prepare(
                    "SELECT * FROM "+TABLE
                    +" WHERE "+Fields.TENANT_KEY+" = :tenant_key"
                    +" AND "+Fields.KEY+" = :key");
            });
        // Prepare the bound statement
        return preparedStatement.bind()
            .setString("tenant_key", tenantKey)
            .setString("key", key);
    }
    
    protected BoundStatement bindUpdateStatement(
        Widget persistedWidget, 
        Widget widget
    ) {
        // Obtain the prepared statement
        PreparedStatement preparedStatement = preparedStatements
            .computeIfAbsent("update", (name) -> {
                return session.prepare(
                    "UPDATE "+TABLE
                    +" SET "+Fields.DESCRIPTION+" = :description"
                    +" WHERE "+Fields.TENANT_KEY+" = :tenant_key"
                    +" AND "+Fields.KEY+" = :key");
            });
        // Prepare the bound statement
        return preparedStatement.bind()
            .setString("tenant_key", persistedWidget.getTenantKey())
            .setString("key", persistedWidget.getKey())
            .setString("description", widget.getDescription());
    }
    
    protected BoundStatement bindDeleteStatement(Widget widget) {
        // Obtain the prepared statement
        PreparedStatement preparedStatement = preparedStatements
            .computeIfAbsent("delete", (name) -> {
                return session.prepare(
                    "DELETE FROM "+TABLE
                    +" WHERE "+Fields.TENANT_KEY+" = :tenant_key"
                    +" AND "+Fields.KEY+" = :key");
            });
        // Prepare the bound statement
        return preparedStatement.bind()
            .setString("tenant_key", widget.getTenantKey())
            .setString("key", widget.getKey());
    }
    
    /*----------------------------------------------------------------------------------------------
     * METHODS
     *--------------------------------------------------------------------------------------------*/

    public CompletableFuture<ImmutableList<Widget>> list(
        String tenantKey,
        Integer limit, 
        String key
    ) {
        // Prepare the statement
        BoundStatement statement = bindListStatement(
            tenantKey,
            limit, 
            key);
        // Execute the bound statement
        return toCompletableFuture(session.executeAsync(statement))
            // Load all rows asynchronously
            .thenCompose(WidgetDao::allRows)
            // Convert the rows to models
            .thenApply(rows -> rows.stream()
                .map(this::toWidget)
                .collect(collectingAndThen(toList(), ImmutableList::copyOf))
            );
    }
    
    public CompletableFuture<Widget> create(
        Widget widget
    ) {
        // Prepare the statement
        BoundStatement statement = bindCreateStatement(widget);
        // Execute the bound statement
        return toCompletableFuture(session.executeAsync(statement))
            // Once the statement has executed, return the model
            .thenApply(resultSet -> widget);
    }
    
    public CompletableFuture<Optional<Widget>> retrieve(
        String tenantKey, 
        String key
    ) {
        // Prepare the statement
        BoundStatement statement = bindRetrieveStatement(tenantKey, key);
        // Execute the bound statement
        return toCompletableFuture(session.executeAsync(statement))
            // Convert the row to an optional model
            .thenApply(resultSet -> {
                Row row = resultSet.one();
                return (row == null) ? Optional.ofNullable(null) : Optional.of(toWidget(row));
            });
    }
    
    public CompletableFuture<Widget> update(
        Widget persistedWidget, 
        Widget widget
    ) {
        // Define the statement to be executed
        Statement statement;
        // If either of the primary key fields changed (indicating that update can't be used)
        if (
            !Objects.equals(persistedWidget.getTenantKey(), widget.getTenantKey())
            || !Objects.equals(persistedWidget.getKey(), widget.getKey())
        ) {
            // Prepare the statement
            statement = new BatchStatement()
                .add(bindDeleteStatement(persistedWidget))
                .add(bindCreateStatement(widget));
        }
        // If the primary key fields are the same (indicating that an update can be used)
        else {
            // Prepare the statement
            statement = bindUpdateStatement(persistedWidget, widget);
        }
        // Execute the bound statement
        return toCompletableFuture(session.executeAsync(statement))
            // Once the statement has executed, return the model
            .thenApply(resultSet -> widget);
    }
    
    public CompletableFuture<Widget> delete(
        Widget widget
    ) {
        // Prepare the statement
        BoundStatement statement = bindDeleteStatement(widget);
        // Execute the bound statement
        return toCompletableFuture(session.executeAsync(statement))
            // Once the statement has executed, return the model
            .thenApply(resultSet -> widget);
    }

    /*----------------------------------------------------------------------------------------------
     * HELPER METHODS
     *--------------------------------------------------------------------------------------------*/
    
    @FunctionalInterface
    public interface IterateRowsUntilFunction<T> {
        public boolean apply(T memo, Row row);
    }
    
    /**
     * This method is used to asynchronously wrap the ResultSet::all call, which may block the 
     * thread when iterating through Cassandra result pages (see
     * https://docs.datastax.com/en/developer/java-driver/3.2/manual/async/#async-paging).
     * 
     * @param resultSet
     * @return 
     */
    public static CompletableFuture<List<Row>> allRows(
        ResultSet resultSet
    ) {
        return allRows(CompletableFuture.completedFuture(resultSet));
    }
    
    /**
     * This method is used to asynchronously wrap the ResultSet::all call, which may block the 
     * thread when iterating through Cassandra result pages (see
     * https://docs.datastax.com/en/developer/java-driver/3.2/manual/async/#async-paging).
     * 
     * @param resultSetFuture
     * @return 
     */
    public static CompletableFuture<List<Row>> allRows(
        CompletableFuture<ResultSet> resultSetFuture
    ) {
        return asynchronouslyStream(resultSetFuture, new ArrayList<>(), (memo, row) -> {
            // Add the row to the memoized result
            memo.add(row);
            // Continue streaming
            return true;
        });
    }
    
    /**
     * This method can be used to asynchronously stream rows from a ResultSet until a certain 
     * criteria is met.
     * 
     * @param <T>
     * @param resultSetFuture
     * @param memo
     * @param iterateRowsUntilFunction
     * @return 
     */
    public static <T> CompletableFuture<T> asynchronouslyStream(
        CompletableFuture<ResultSet> resultSetFuture, 
        T memo, 
        IterateRowsUntilFunction<T> iterateRowsUntilFunction
    ) {
        return resultSetFuture.thenCompose(resultSet -> {
            int remainingInPage = resultSet.getAvailableWithoutFetching();

            boolean continueIterating = false;
            for (Row row : resultSet) {
                continueIterating = iterateRowsUntilFunction.apply(memo, row);
                if (--remainingInPage == 0 || !continueIterating)
                    break;
            }

            CompletableFuture<T> result;
            boolean wasLastPage = resultSet.getExecutionInfo().getPagingState() == null;
            if (!continueIterating || wasLastPage) {
                result = CompletableFuture.completedFuture(memo);
            } else {
                result = asynchronouslyStream(
                    toCompletableFuture(resultSet.fetchMoreResults()),
                    memo,
                    iterateRowsUntilFunction
                );
            }
            return result;
        });
    }
    
    /**
     * Basic implementation from https://dzone.com/articles/converting-listenablefutures
     * 
     * @param <T>
     * @param listenableFuture
     * @return 
     */
    public static <T> CompletableFuture<T> toCompletableFuture(
        final ListenableFuture<T> listenableFuture
    ) {
        // Create the completable future
        CompletableFuture<T> completable = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean result = listenableFuture.cancel(mayInterruptIfRunning);
                super.cancel(mayInterruptIfRunning);
                return result;
            }
        };
        // Create the future callback
        FutureCallback<T> callback = new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                completable.complete(result);
            }
            @Override
            public void onFailure(Throwable t) {
                completable.completeExceptionally(t);
            }
        };
        // Add the callback to the listenable future
        Futures.addCallback(listenableFuture, callback, ForkJoinPool.commonPool());
        // Return the completable future
        return completable;
    }
    
}
