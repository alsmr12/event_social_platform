package models

import (
	"gorm.io/gorm"
	"time"
)

type SocialLink struct {
	ID         uint           `gorm:"primaryKey" json:"id"`
	UserID     uint           `gorm:"not null;index" json:"user_id"`
	User       User           `gorm:"foreignKey:UserID" json:"-"`
	Platform   string         `gorm:"size:50;not null" json:"platform"`
	Username   string         `gorm:"size:100;not null" json:"username"`
	CustomName string         `gorm:"size:50" json:"custom_name"`
	CreatedAt  time.Time      `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt  time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
	DeletedAt  gorm.DeletedAt `gorm:"index" json:"-"`
}

const (
	PlatformVK     = "vk"
	PlatformTG     = "tg"
	PlatformCustom = "custom"
)
