/*
 * Copyright (C) 2021 Dr.NooB
 *
 * This file is a part of Data Monitor <https://github.com/itsdrnoob/DataMonitor>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.drnoob.datamonitor.adapters.data;

import java.io.Serializable;

import javax.annotation.Nullable;

public class TranslatorModel implements Serializable {
    private String title, summary;
    private int iconID;
    private boolean hasLinkedGithub;
    private String githubLink;

    public TranslatorModel() {
    }

    public TranslatorModel(String title, String summary, int iconID, boolean hasLinkedGithub, @Nullable String githubLink) {
        this.title = title;
        this.summary = summary;
        this.iconID = iconID;
        this.hasLinkedGithub = hasLinkedGithub;
        this.githubLink = githubLink;
    }

    public TranslatorModel(String title, String summary, int iconID, boolean hasLinkedGithub) {
        this.title = title;
        this.summary = summary;
        this.iconID = iconID;
        this.hasLinkedGithub = hasLinkedGithub;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getIconID() {
        return iconID;
    }

    public void setIconID(int iconID) {
        this.iconID = iconID;
    }

    public boolean hasLinkedGithub() {
        return hasLinkedGithub;
    }

    public void setHasLinkedGithub(boolean hasLinkedGithub) {
        this.hasLinkedGithub = hasLinkedGithub;
    }

    public String getGithubLink() {
        return githubLink;
    }

    public void setGithubLink(String githubLink) {
        this.githubLink = githubLink;
    }
}
