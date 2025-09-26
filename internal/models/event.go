package models

import (
	"gorm.io/gorm"
	"time"
)

type Event struct {
	ID              uint           `gorm:"primaryKey" json:"id"`
	Title           string         `gorm:"not null;size:200" json:"title"`
	Description     string         `gorm:"type:text" json:"description"`
	Type            string         `gorm:"not null;size:50" json:"type"` // concert, lecture, sport, etc.
	DateTime        time.Time      `gorm:"not null" json:"date_time"`
	Location        string         `gorm:"not null" json:"location"`
	Latitude        float64        `json:"latitude"`
	Longitude       float64        `json:"longitude"`
	CreatorID       uint           `gorm:"not null" json:"creator_id"`
	Creator         User           `gorm:"foreignKey:CreatorID" json:"creator"`
	IsPrivate       bool           `gorm:"default:false" json:"is_private"`
	InviteCode      string         `gorm:"size:100" json:"invite_code"`
	MaxParticipants int            `json:"max_participants"`
	CreatedAt       time.Time      `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt       time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
	DeletedAt       gorm.DeletedAt `gorm:"index" json:"-"`
}

type CreateEventRequest struct {
	Title           string `form:"title" binding:"required"`
	Description     string `form:"description" binding:"required"`
	Type            string `form:"type" binding:"required"`
	DateTime        string `form:"date_time" binding:"required"`
	Location        string `form:"location" binding:"required"`
	Latitude        string `form:"latitude"`
	Longitude       string `form:"longitude"`
	IsPrivate       bool   `form:"is_private"`
	MaxParticipants int    `form:"max_participants"`
}
