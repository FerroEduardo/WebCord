package com.ferroeduardo.webcord.entity;

import com.ferroeduardo.webcord.listener.WebsiteStatus;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "WebsiteRecord")
public class WebsiteRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp")
    private ZonedDateTime dateTime;

    @Column(name = "websiteName", nullable = false)
    private String websiteName;

    @Column(name = "websiteStatus", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private WebsiteStatus websiteStatus;

    public WebsiteRecord() {
    }

    public WebsiteRecord(Long id, ZonedDateTime dateTime, String websiteName, WebsiteStatus websiteStatus) {
        this.id = id;
        this.dateTime = dateTime;
        this.websiteName = websiteName;
        this.websiteStatus = websiteStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getWebsiteName() {
        return websiteName;
    }

    public void setWebsiteName(String websiteName) {
        this.websiteName = websiteName;
    }

    public WebsiteStatus getWebsiteStatus() {
        return websiteStatus;
    }

    public void setWebsiteStatus(WebsiteStatus websiteStatus) {
        this.websiteStatus = websiteStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebsiteRecord that = (WebsiteRecord) o;
        return id.equals(that.id) && dateTime.equals(that.dateTime) && websiteName.equals(that.websiteName) && websiteStatus.equals(that.websiteStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateTime, websiteName, websiteStatus);
    }

    @Override
    public String toString() {
        return "WebsiteRecord{" +
                "id=" + id +
                ", dateTime=" + dateTime +
                ", websiteName='" + websiteName + '\'' +
                ", websiteStatus=" + websiteStatus +
                '}';
    }
}
