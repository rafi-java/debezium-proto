/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.services;

import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemStreamPartition;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.StreamTask;
import org.apache.samza.task.TaskCoordinator;
import org.debezium.core.annotation.NotThreadSafe;
import org.debezium.core.component.DatabaseId;
import org.debezium.core.component.EntityId;
import org.debezium.core.component.EntityType;
import org.debezium.core.component.Identifier;
import org.debezium.core.component.Schema;
import org.debezium.core.doc.Document;
import org.debezium.core.message.Message;

/**
 * A service (or task in Samza parlance) responsible for re-partitioning the changes to entities and schemas onto a single
 * topic (partitioned by entity type) that the {@link EntityLearningService} can consume.
 * <p>
 * This service consumes two streams:
 * <ol>
 * <li>The "{@link Streams#entityUpdates entity-updates}" topic (partitioned by entity type) that contains the successfully
 * applied patch and the updated entity representation; each messages for which {@link Message#isLearningEnabled(Document)
 * learning is enabled} is simply copied as-is onto the "{@link Streams#schemaLearning schema-learning}" topic (partitioned by
 * entity type).</li>
 * <li>The "{@link Streams#schemaUpdates schema-updates}" topic (partitioned by database ID) that contains the successfully
 * applied patch and updated schema representation; each patch message is disected into a separate read-requests for each entity
 * type, and placed onto the "{@link Streams#schemaLearning schema-learning}" topic (partitioned by entity type).</li>
 * </ol>
 * <p>
 * <em>Note: to ensure that the schema changes are accepted as quickly as possible, the schema updates should be prioritized
 * higher than the entity updates.</em>
 * <p>
 * This service forwards these messages on to the "{@link Streams#schemaLearning schema-learning}" topic, partitioned by entity
 * type. Entity updates are forwarded as-is, but each updated schema component is sent separately as a completed patch request.
 * 
 * @author Randall Hauch
 */
@NotThreadSafe
public class EntityLearningPartitioner implements StreamTask {
    
    @Override
    public void process(IncomingMessageEnvelope envelope, MessageCollector collector, TaskCoordinator coordinator) throws Exception {
        SystemStreamPartition stream = envelope.getSystemStreamPartition();
        if (Streams.isEntityUpdates(stream)) {
            processEntityUpdate(envelope, collector, coordinator);
        } else if (Streams.isSchemaUpdates(stream)) {
            processSchemaUpdate(envelope, collector, coordinator);
        }
    }
    
    private void processSchemaUpdate(IncomingMessageEnvelope env, MessageCollector collector, TaskCoordinator coordinator)
            throws Exception {
        DatabaseId dbId = Identifier.parseDatabaseId(env.getKey());
        Document message = (Document) env.getMessage();
        Document schema = Message.getAfter(message);
        assert schema != null;
        
        if (Schema.isLearningEnabled(schema)) {
            // Send each entity type within the schema via a separate read message onto the output stream,
            // partitioned by entity type...
            Schema.onEachEntityType(schema, dbId, (type, typeDoc) -> {
                Document typeMessage = Message.createResponseFromRequest(message);
                Message.setAfter(message, typeDoc);
                collector.send(new OutgoingMessageEnvelope(Streams.schemaLearning(dbId), type, type, typeMessage));
            });
        }
    }
    
    private void processEntityUpdate(IncomingMessageEnvelope env, MessageCollector collector, TaskCoordinator coordinator)
            throws Exception {
        Document message = (Document) env.getMessage();
        if (Message.isLearningEnabled(message)) {
            EntityId entityId = Identifier.parseEntityId(env.getKey());
            DatabaseId dbId = entityId.databaseId();
            EntityType type = entityId.type();
            
            // Send the patch response to the output stream, partitioned by the entity type ...
            collector.send(new OutgoingMessageEnvelope(Streams.schemaLearning(dbId), type, entityId, message));
        }
    }
    
}
