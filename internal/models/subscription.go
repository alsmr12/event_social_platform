package models

import (
	"gorm.io/gorm"
	"time"
)

type Subscription struct {
	ID          uint           `gorm:"primaryKey" json:"id"`
	FollowerID  uint           `gorm:"not null" json:"follower_id"` // Тот, кто подписывается
	Follower    User           `gorm:"foreignKey:FollowerID" json:"follower"`
	FollowingID uint           `gorm:"not null" json:"following_id"` // Тот, на кого подписываются
	Following   User           `gorm:"foreignKey:FollowingID" json:"following"`
	CreatedAt   time.Time      `gorm:"autoCreateTime" json:"created_at"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"-"`
}

// Уникальный индекс чтобы нельзя было подписаться дважды
func (Subscription) TableName() string {
	return "subscriptions"
}
