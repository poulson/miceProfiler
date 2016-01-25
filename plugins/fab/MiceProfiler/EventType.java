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

public enum EventType
{

    CONTACT,
    DISTANCE_INFERIOR_THRESHOLD_2,    
    ORAL_ORAL,
    ORAL_A_GENITAL_B,
    ORAL_B_GENITAL_A,
    A_BEHIND_B,
    B_BEHIND_A,
    BESIDE_SAME_WAY,
    BESIDE_OPPOSITE_WAY,
    A_SPEED_HIGHER_THAN_B_SPEED_AND_A_GET_TO_B,
    B_SPEED_HIGHER_THAN_A_SPEED_AND_B_GET_TO_A,
    A_SPEED_HIGHER_THAN_B_SPEED_AND_A_ESCAPE_B,
    B_SPEED_HIGHER_THAN_A_SPEED_AND_B_ESCAPE_A,
    A_GOTO_B_FINISH_CONTACT_START_NO_CONTACT,
    B_GOTO_A_FINISH_CONTACT_START_NO_CONTACT,
    A_ESCAPE_AFTER_CONTACT_FINISH_WITH_NO_CONTACT,
    B_ESCAPE_AFTER_CONTACT_FINISH_WITH_NO_CONTACT,
    A_GOTO_B_AND_B_ESCAPE_THRESHOLD_IN_OUT,
    B_GOTO_A_AND_A_ESCAPE_THRESHOLD_IN_OUT,
    A_GOTO_B_AND_A_ESCAPE_THRESHOLD_IN_OUT,
    B_GOTO_A_AND_B_ESCAPE_THRESHOLD_IN_OUT,
    A_FOLLOW_B,
    MOUSE_A_SPEED,
    MOUSE_B_SPEED,
    A_BEHIND_B_AND_B_BEHIND_A,
    B_CAN_SEE_A,
    A_CAN_SEE_B,
    A_STOP,
    B_STOP,
    DISTANCE_INFERIOR_THRESHOLD_1,
    USER_EVENT;

}
