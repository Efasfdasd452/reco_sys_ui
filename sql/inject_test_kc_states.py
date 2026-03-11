"""
注入测试用例 6-10 的 KC 状态到 user_kc_state 表。

与项目逻辑对应：表结构及列名需与 JPA 实体 UserKcState 一致
  (org.reco.reco_sys.module.learning.entity.UserKcState)：
  user_id, kp_id, mastery_level, correct_count, total_count, updated_at
  (kp_id 对应 knowledge_point.id，由 knowledge_point.py_kc_index 查得)

用法：
  python inject_test_kc_states.py <userId> <testCase>

示例（为 MySQL userId=5 的账号注入测试用例6的状态）：
  python inject_test_kc_states.py 5 6

注意：
- testCase 取值：6 / 7 / 8 / 9 / 10
- 脚本会先删除该用户已有的所有 KC 状态，再插入新状态
- correct_count/total_count 按 mastery_level 折算为 correct_count/total_count=mlkc，满足 NOT NULL
- pkc 在 Java 侧自动计算为 1-mlkc，与原始 pyKT pkc 有差异
- 用例 6/9/10 的 pkc[kc75]=0 特性在当前 Java 近似下无法完全还原
"""

import sys
import pymysql

# ── DB 配置 ─────────────────────────────────────────────────────────────────
DB = dict(host='192.168.50.129', port=3306, user='root',
          password='123456', db='reco_sys', charset='utf8mb4')

# ── 各测试用例的 KC 掌握度（仅文档中明确列出的关键 KC）────────────────────
# key = pyKcIndex (0-based), value = mlkc (0.0~1.0)
# 文档中未列出的 KC 表示"未学习过"，不插入记录（等同于 mlkc=0）
TEST_CASE_STATES = {
    6: {
        # uid102：答对 45/112 KC，kc81 低掌握+高频 → 推荐排第一
        # 预期 Top1: ex189 (kc81)
        80: 0.00,   # 最薄弱
        19: 0.02,
         3: 0.03,
        75: 0.53,   # 原始pkc=0（Java近似pkc=0.47，无法还原pkc=0）
        81: 0.12,   # 低掌握+高频 → 推荐排第一
        82: 0.72,   # 原始pkc=0
        92: 0.63,
        48: 0.88,
        83: 0.88,
    },
    7: {
        # uid110：答对 89/112 KC，推荐最多样
        # 预期 Top1: ex192 (kc81+kc82)，Top5: ex629 (kc3+kc17)
        80: 0.00,
        19: 0.02,
         3: 0.03,   # pkc=91%
        17: 0.48,   # pkc=95%
        75: 0.53,   # pkc=85%
        81: 0.12,   # pkc=84%
        82: 0.72,   # pkc=89%
        92: 0.63,   # pkc=96%
        48: 0.88,
        83: 0.88,
        # 为模拟"答对89个KC"，补充部分已掌握的KC
         0: 0.85,  1: 0.90,  2: 0.85,  4: 0.82,  5: 0.83,
         6: 0.88,  7: 0.80,  8: 0.84,  9: 0.86, 10: 0.85,
        11: 0.83, 12: 0.81, 13: 0.82, 14: 0.79, 15: 0.80,
        16: 0.84, 18: 0.83, 20: 0.85, 21: 0.86, 22: 0.84,
        23: 0.82, 24: 0.81, 25: 0.83, 26: 0.80, 27: 0.82,
        28: 0.79, 29: 0.84, 30: 0.83, 31: 0.81, 32: 0.80,
        33: 0.85, 34: 0.82, 35: 0.03, 36: 0.80, 37: 0.81,
        38: 0.83, 39: 0.84, 40: 0.82, 41: 0.80, 42: 0.81,
        43: 0.85, 44: 0.86, 45: 0.83, 46: 0.82, 47: 0.84,
        49: 0.85, 50: 0.83, 51: 0.82, 52: 0.84, 53: 0.85,
        54: 0.83, 55: 0.81, 56: 0.80, 57: 0.82, 58: 0.83,
        59: 0.84, 60: 0.85, 61: 0.83, 62: 0.82, 63: 0.84,
        64: 0.85, 65: 0.83, 66: 0.82, 67: 0.81, 68: 0.80,
        69: 0.82, 70: 0.83, 71: 0.84, 72: 0.85, 73: 0.83,
        74: 0.82, 76: 0.83, 77: 0.84, 78: 0.85, 79: 0.83,
        84: 0.82, 85: 0.84, 86: 0.85, 87: 0.83, 88: 0.82,
        89: 0.81,
    },
    8: {
        # uid197：答对 97/112 KC，kc20 极薄弱 → Top1~9 全是 kc20
        # 预期 Top1: ex733 (kc20)，Top1~9 几乎全是 kc20
        20: 0.19,   # 低掌握+高频 → 独占 Top1~9
         3: 0.01,
        19: 0.01,
        75: 0.53,   # pkc=84%
        92: 0.59,   # pkc=93%
        # 补充大量已掌握的KC（97个）
         0: 0.85,  1: 0.90,  2: 0.85,  4: 0.82,  5: 0.83,
         6: 0.88,  7: 0.80,  8: 0.84,  9: 0.86, 10: 0.85,
        11: 0.83, 12: 0.81, 13: 0.82, 14: 0.79, 15: 0.80,
        16: 0.84, 17: 0.85, 18: 0.83, 21: 0.86, 22: 0.84,
        23: 0.82, 24: 0.81, 25: 0.83, 26: 0.80, 27: 0.82,
        28: 0.79, 29: 0.84, 30: 0.83, 31: 0.81, 32: 0.80,
        33: 0.85, 34: 0.82, 35: 0.80, 36: 0.83, 37: 0.81,
        38: 0.80, 39: 0.82, 40: 0.81, 41: 0.83, 42: 0.84,
        43: 0.85, 44: 0.86, 45: 0.83, 46: 0.82, 47: 0.84,
        48: 0.88, 49: 0.85, 50: 0.83, 51: 0.82, 52: 0.84,
        53: 0.85, 54: 0.83, 55: 0.81, 56: 0.80, 57: 0.82,
        58: 0.83, 59: 0.84, 60: 0.85, 61: 0.83, 62: 0.82,
        63: 0.84, 64: 0.85, 65: 0.83, 66: 0.82, 67: 0.81,
        68: 0.80, 69: 0.82, 70: 0.83, 71: 0.84, 72: 0.85,
        73: 0.83, 74: 0.82, 76: 0.83, 77: 0.84, 78: 0.85,
        79: 0.83, 80: 0.00, 81: 0.82, 82: 0.84, 83: 0.88,
        84: 0.82, 85: 0.84, 86: 0.85, 87: 0.83, 88: 0.82,
        89: 0.81, 90: 0.80, 91: 0.82, 93: 0.83, 94: 0.84,
        95: 0.83, 96: 0.82, 97: 0.81, 98: 0.80, 99: 0.82,
        100: 0.83, 101: 0.84, 102: 0.85, 103: 0.83, 104: 0.82,
        105: 0.81, 106: 0.80, 107: 0.82,
    },
    9: {
        # uid501：答对 27/112 KC，pkc[kc75]=0 → Top1: ex1066 (kc14+kc15)
        # 注意：Java pkc=1-mlkc，kc75 pkc≠0，结果可能有偏差
        14: 0.46,   # pkc=58%
        15: 0.62,   # pkc=78%
        75: 0.51,   # 原始pkc=0，Java近似pkc=0.49
        92: 0.57,   # pkc=67%
        80: 0.00,
        # 补充部分已掌握KC（约27个）
         0: 0.82,  1: 0.85,  2: 0.80,  3: 0.03,  4: 0.81,
         5: 0.83,  6: 0.84,  7: 0.80,  8: 0.82,  9: 0.81,
        10: 0.83, 11: 0.82, 12: 0.80, 13: 0.81, 16: 0.82,
        17: 0.80, 18: 0.83, 19: 0.02, 20: 0.82, 21: 0.81,
        22: 0.80, 23: 0.82, 24: 0.81, 25: 0.83, 26: 0.82,
        27: 0.80,
    },
    10: {
        # uid503：答对 30/112 KC，pkc[kc75]=0 → Top1: ex200 (kc13)
        # 注意：Java pkc=1-mlkc，kc75 pkc≠0，结果可能有偏差
        13: 0.26,   # 低掌握+pkc=37% → 排第一
        75: 0.53,   # 原始pkc=0，Java近似pkc=0.47
        92: 0.63,   # pkc=94%，掌握度偏高被压制
        14: 0.50,   # pkc较高，出现在Top4~8
        15: 0.60,
        80: 0.00,
        # 补充部分已掌握KC（约30个）
         0: 0.82,  1: 0.85,  2: 0.80,  3: 0.03,  4: 0.81,
         5: 0.83,  6: 0.84,  7: 0.80,  8: 0.82,  9: 0.81,
        10: 0.83, 11: 0.82, 12: 0.80, 16: 0.82, 17: 0.80,
        18: 0.83, 19: 0.02, 20: 0.82, 21: 0.81, 22: 0.80,
        23: 0.82, 24: 0.81, 25: 0.83, 26: 0.82, 27: 0.80,
        28: 0.81, 29: 0.82, 30: 0.80,
    },
}

EXPECTED_TOP1 = {
    6:  'ex189  (MySQL id=190, kc81)',
    7:  'ex192  (MySQL id=193, kc81+kc82)',
    8:  'ex733  (MySQL id=734, kc20)',
    9:  'ex1066 (MySQL id=1067, kc14+kc15)',
    10: 'ex200  (MySQL id=201, kc13)',
}


def main():
    if len(sys.argv) != 3:
        print('用法: python inject_test_kc_states.py <userId> <testCase(6~10)>')
        sys.exit(1)

    user_id   = int(sys.argv[1])
    test_case = int(sys.argv[2])

    if test_case not in TEST_CASE_STATES:
        print(f'错误：testCase 必须为 6~10，当前为 {test_case}')
        sys.exit(1)

    kc_states = TEST_CASE_STATES[test_case]

    conn = pymysql.connect(**DB)
    try:
        with conn.cursor() as cur:
            # 1. 查询 kpId：根据 py_kc_index 从 knowledge_point 表查出 MySQL id
            py_indices = list(kc_states.keys())
            fmt = ','.join(['%s'] * len(py_indices))
            cur.execute(
                f'SELECT id, py_kc_index FROM knowledge_point WHERE py_kc_index IN ({fmt})',
                py_indices
            )
            rows = cur.fetchall()
            if not rows:
                print('错误：knowledge_point 表中未找到任何 KC，请先完成数据初始化（管理员页面→导入推荐数据）')
                sys.exit(1)

            idx_to_kp_id = {row[1]: row[0] for row in rows}
            print(f'找到 {len(idx_to_kp_id)} 个 KC（共需 {len(py_indices)} 个）')
            missing = set(py_indices) - set(idx_to_kp_id.keys())
            if missing:
                print(f'警告：以下 pyKcIndex 在数据库中不存在，已跳过：{sorted(missing)}')

            # 2. 清除该用户的旧 KC 状态
            cur.execute('DELETE FROM user_kc_state WHERE user_id = %s', (user_id,))
            deleted = cur.rowcount
            print(f'已清除用户 {user_id} 的旧 KC 状态（{deleted} 条）')

            # 3. 插入新 KC 状态（与实体 UserKcState 一致：含 correct_count/total_count，mlkc = correct_count/total_count）
            inserted = 0
            for py_idx, mlkc in kc_states.items():
                kp_id = idx_to_kp_id.get(py_idx)
                if kp_id is None:
                    continue
                total = 100
                correct = max(0, min(total, round(mlkc * total)))
                cur.execute(
                    '''INSERT INTO user_kc_state (user_id, kp_id, mastery_level, correct_count, total_count, updated_at)
                       VALUES (%s, %s, %s, %s, %s, NOW())''',
                    (user_id, kp_id, mlkc, correct, total)
                )
                inserted += 1

            conn.commit()
            print(f'注入完成：{inserted} 条 KC 状态')
            print(f'\n测试用例 {test_case} 注入完成！')
            print(f'预期推荐 Top1：{EXPECTED_TOP1[test_case]}')
            print(f'\n注意：pkc 在 Java 侧计算为 1-mlkc，与原始 pyKT pkc 有差异。')
            if test_case in (6, 9, 10):
                print(f'用例 {test_case} 依赖 pkc[kc75]=0 特性，Java 近似无法完全还原，推荐结果可能略有偏差。')
    finally:
        conn.close()


if __name__ == '__main__':
    main()
