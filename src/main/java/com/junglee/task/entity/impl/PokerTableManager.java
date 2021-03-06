package com.junglee.task.entity.impl;

import com.junglee.task.entity.GameStateManagerService;
import com.junglee.task.event.Event;
import com.junglee.task.event.EventHandler;
import com.junglee.task.event.Events;
import com.junglee.task.service.ChatService;
import com.junglee.task.service.DBLookupService;
import com.junglee.task.session.SessionFactory;
import com.junglee.task.session.id.UniqueIDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by vishwasourabh.sahay on 19/02/17.
 */
@Component
public class PokerTableManager implements EventHandler {

    private com.junglee.task.service.DBLookupService dbLookupService;
    private UniqueIDService uniqueIDService;
    private GameStateManagerService gameStateManagerService;
    private SessionFactory sessionFactory;
    private GameChannelSession.GameChannelSessionBuilder sessionBuilder;
    private ChatService chatService;

    @Autowired
    public PokerTableManager( DBLookupService DBLookupService, UniqueIDService uniqueIDService, GameStateManagerService
                             gameStateManagerService, SessionFactory sessionFactory, ChatService chatService) {
        this.dbLookupService = DBLookupService;
        this.uniqueIDService = uniqueIDService;
        this.gameStateManagerService = gameStateManagerService;
        this.sessionFactory = sessionFactory;
        sessionBuilder = new GameChannelSession.GameChannelSessionBuilder(uniqueIDService);
        this.chatService = chatService;
    }

    List<PokerTable> usedTableList = Collections.synchronizedList( new ArrayList<PokerTable>());
    List<PokerTable> avaiableTableList = Collections.synchronizedList (new ArrayList<PokerTable>());


    public PokerTable createOrGetTable()
    {
       if(avaiableTableList.isEmpty())
       {
           synchronized (this)
           {
               if(avaiableTableList.isEmpty()) //do
               {
                   return createNewPokerTable();
               }
           }
       }
       else
       {

           Iterator iterator = avaiableTableList.iterator();
           while(iterator.hasNext())
           {
               PokerTable pokerTable = (PokerTable) iterator.next();
               if(pokerTable.canTakeMorePlayers())
               {
                   return pokerTable;
               }
           }
       }
        return createNewPokerTable();
    }

    private PokerTable createNewPokerTable() {
        PokerTable table = new PokerTable(sessionBuilder, gameStateManagerService, sessionFactory, chatService, dbLookupService
        , this);// pass its own instance, so it can get events from sessions.
        avaiableTableList.add(table);
        return table;
    }

    public void onEvent(Event event) {

        switch(event.getType())
        {
            case Events.START:
                populateUsedTables(event);
                break;
            case Events.STOP:
                 returnTableToAvailableTablePool(event);
                break;


        }

    }

    private void returnTableToAvailableTablePool(Event event) {
        PokerTable table = (PokerTable) event.getEventContext().getAttachment();
        synchronized (this)
        {
            usedTableList.remove(table);
            avaiableTableList.add(table);
        }
    }

    private void populateUsedTables(Event event) {
        PokerTable table = (PokerTable) event.getEventContext().getAttachment();
        synchronized (this)
        {
            avaiableTableList.remove(table);
            usedTableList.add(table);
        }
    }

    public int getEventType() {
        return Events.ANY;
    }
}
