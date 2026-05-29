import { NavLink } from 'react-router-dom'

/**
 * 학생 하단 탭 — 홈 · 클리닉 · 기록 · MY
 * 아이콘은 stroke 기반 outline 으로 통일 (DESIGN_SYSTEM §컴포넌트).
 */
const ICON_PROPS = {
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
}

const TABS = [
  {
    to: '/student',
    end: true,
    label: '홈',
    icon: (
      <svg {...ICON_PROPS}>
        <path d="M3.5 11.2 12 4l8.5 7.2V20a1 1 0 0 1-1 1h-4.8v-6h-5.4v6H4.5a1 1 0 0 1-1-1v-8.8Z" />
      </svg>
    ),
  },
  {
    to: '/student/clinic',
    label: '클리닉',
    icon: (
      <svg {...ICON_PROPS}>
        <rect x="3.5" y="5" width="17" height="15.5" rx="2.5" />
        <path d="M3.5 9.5h17M8 3.5v3.2M16 3.5v3.2" />
      </svg>
    ),
  },
  {
    to: '/student/records',
    label: '기록',
    icon: (
      <svg {...ICON_PROPS}>
        <path d="M5 19.5V8M10 19.5V4M15 19.5v-8M20 19.5h-17" />
      </svg>
    ),
  },
  {
    to: '/student/my',
    label: 'MY',
    icon: (
      <svg {...ICON_PROPS}>
        <circle cx="12" cy="8.5" r="3.8" />
        <path d="M4.8 20.5c.6-3.7 3.7-5.6 7.2-5.6s6.6 1.9 7.2 5.6" />
      </svg>
    ),
  },
]

export default function BottomNav() {
  return (
    <nav className="ams-bottom-nav" aria-label="주요 메뉴">
      <div className="ams-bottom-nav__inner">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            end={tab.end}
            className={({ isActive }) =>
              isActive
                ? 'ams-bottom-nav__item ams-bottom-nav__item--active'
                : 'ams-bottom-nav__item'
            }
          >
            <span className="ams-bottom-nav__icon">{tab.icon}</span>
            <span className="ams-bottom-nav__label">{tab.label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
