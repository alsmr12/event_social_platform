package models

import (
	"gorm.io/gorm"
	"time"
)

type Achievement struct {
	ID          uint      `gorm:"primaryKey" json:"id"`
	Name        string    `gorm:"not null;size:200" json:"name"`
	Description string    `gorm:"type:text" json:"description"`
	Icon        string    `gorm:"size:100" json:"icon"`
	Points      int       `gorm:"not null" json:"points"`
	Type        string    `gorm:"not null;size:50" json:"type"`
	Condition   int       `gorm:"not null" json:"condition"`
	CreatedAt   time.Time `gorm:"autoCreateTime" json:"created_at"`
}

type UserAchievement struct {
	ID            uint           `gorm:"primaryKey" json:"id"`
	UserID        uint           `gorm:"not null" json:"user_id"`
	AchievementID uint           `gorm:"not null" json:"achievement_id"`
	User          User           `gorm:"foreignKey:UserID" json:"user"`
	Achievement   Achievement    `gorm:"foreignKey:AchievementID" json:"achievement"`
	Progress      int            `gorm:"default:0" json:"progress"`
	Completed     bool           `gorm:"default:false" json:"completed"`
	CompletedAt   *time.Time     `json:"completed_at"`
	CreatedAt     time.Time      `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt     time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
	DeletedAt     gorm.DeletedAt `gorm:"index" json:"-"`
}
