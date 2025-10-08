package models

import (
	"gorm.io/gorm"
	"time"
)

type EventSubscription struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	UserID    uint           `gorm:"not null" json:"user_id"`    // Кто подписывается
	EventID   uint           `gorm:"not null" json:"event_id"`   // На какое событие
	User      User           `gorm:"foreignKey:UserID" json:"user"`
	Event     Event          `gorm:"foreignKey:EventID" json:"event"`
	CreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

// Уникальный индекс чтобы один пользователь не мог подписаться дважды на одно событие
func (EventSubscription) TableName() string {
	return "event_subscriptions"
}