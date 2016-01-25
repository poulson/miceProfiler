/*
 * Copyright 2011, 2012 Institut Pasteur.
 * 
 * This file is part of MiceProfiler.
 * 
 * MiceProfiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MiceProfiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MiceProfiler. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.fab.MiceProfiler;

import java.util.ArrayList;
import java.util.HashMap;

public class EventTimeLine
{

    public String criteriaName;
    public EventType eventType;
    public TimeLineCategory timeLineCategory;

    ArrayList<EventCriteria> eventList = new ArrayList<EventCriteria>();

    HashMap<Integer, Double> hashValue = new HashMap<Integer, Double>();

    public EventTimeLine(String criteriaName, EventType eventType, TimeLineCategory timeLineCategory)
    {
        this.timeLineCategory = timeLineCategory;
        this.criteriaName = criteriaName;
        this.eventType = eventType;
    }

    public void addValue(int frame, double value)
    {
        hashValue.put(frame, value);
    }

    public void removeValue(int frame)
    {
        hashValue.remove(frame);
    }

    public double getValueAt(int frame)
    {
        final Double value = hashValue.get(frame);
        if (value == null)
            return 0;
        return value;
    }

    public void addEvent(EventCriteria eventCriteria)
    {
        eventList.add(eventCriteria);
    }

    public void removeEvent(EventCriteria eventCriteria)
    {
        eventList.remove(eventCriteria);
    }

    // look if a previous event exist with t-1.
    // if yes extends previous event.
    // if not create new event.

    public void addPunctualEvent(int t)
    {
        for (final EventCriteria event : eventList)
        {
            if (event.endFrame == t - 1)
            {
                event.endFrame = t;
                return;
            }
        }

        for (final EventCriteria event : eventList)
        {
            if (event.startFrame == t + 1)
            {
                event.startFrame = t;
                return;
            }
        }
        
        // nothing found
        // creates a new event

        final EventCriteria event = new EventCriteria(eventType, t, t);
        addEvent(event);

    }

    public void removeEventLessThanLength(int length)
    {

        for (final EventCriteria eventCriteria : (ArrayList<EventCriteria>) eventList.clone())
        {
            if (eventCriteria.getLength() < length)
            {
                eventList.remove(eventCriteria);
            }
        }

    }

    public double getMeanValue(int tStart, int tEnd)
    {
        int nbValue = 0;
        double total = 0;
        for (int t = tStart; t <= tEnd; t++)
        {
            total += getValueAt(t);
            nbValue++;
        }

        if (nbValue == 0)
            return 0;

        return total / nbValue;

    }

    public double getNbEvent(int tStart, int tEnd)
    {
        double nbEvent = 0;

        for (final EventCriteria eventCriteria : eventList)
        {
            if (eventCriteria.startFrame >= tStart && eventCriteria.startFrame <= tEnd)
            {
                nbEvent++;
            }

            if (eventCriteria.startFrame < tStart && eventCriteria.endFrame > tEnd)
            {
                nbEvent++;
            }
        }

        return nbEvent;

    }

    public double getDensity(int tStart, int tEnd)
    {

        double nbEvent = 0;

        for (int t = tStart; t <= tEnd; t++)
        {
            for (final EventCriteria eventCriteria : eventList)
            {
                if (eventCriteria.startFrame <= t && eventCriteria.endFrame >= t)
                {
                	if ( tStart == 0 ){
//                		System.out.println("Start: " + eventCriteria.startFrame + " end: " + eventCriteria.endFrame );
                	}
                	
                    nbEvent++;
                    break;
                }
            }
        }
        final double ret = nbEvent / (tEnd - tStart);

        System.out.println("density: " + tStart + " : " + tEnd + ":" + nbEvent + ":" + ret );
        
        return ret;
    }

    public double getLengthEvent(int tStart, int tEnd)
    {

        double totalLength = 0;
        double nbEvent = 0;

        for (final EventCriteria eventCriteria : eventList)
        {

            if (eventCriteria.startFrame >= tStart && eventCriteria.startFrame <= tEnd)
            {
                totalLength += eventCriteria.getLength();
                nbEvent++;
            }

            if (eventCriteria.startFrame < tStart && eventCriteria.endFrame > tEnd)
            {
                totalLength += eventCriteria.getLength();
                nbEvent++;
            }
        }

        if (nbEvent == 0)
            return 0;

        return totalLength / nbEvent;

    }

}
