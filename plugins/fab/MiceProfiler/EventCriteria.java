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

public class EventCriteria
{

    EventType eventType;
    int startFrame = 0;
    int endFrame = 0;

    public EventCriteria(EventType eventType, int startFrame, int endFrame)
    {

        this.eventType = eventType;
        this.startFrame = startFrame;
        this.endFrame = endFrame;

    }

    public int getLength()
    {
        return endFrame - startFrame + 1;
    }

}
