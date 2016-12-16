/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.leagues;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@JsonAutoDetect
public class Leagues
{

    private List<League> leagues;

    @JsonProperty
    @XmlElement(name = "league")
    public List<League> getLeagues()
    {
        return leagues;
    }

    public void setLeagues(List<League> leagues)
    {
        this.leagues = leagues;
    }

    public League getLeague(String id)
    {
        for (League league : leagues)
        {
            if (league.getId().equals(id))
            {
                return league;
            }
        }
        return null;
    }

    public boolean deleteLeague(String id)
    {
        return leagues.remove(new League(id));
    }

}
