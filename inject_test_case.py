"""
测试用例注入脚本 — 用于复现 test_cases_recommendation.md 中的用例 6~10
将指定 Python 学生 uid 的知识状态注入到 MySQL，让 Java 推荐系统可以测试。

用法：
    python -X utf8 inject_test_case.py <py_uid> <mysql_user_id>

示例（把 uid102 的状态注入到 mysql user_id=3）：
    python -X utf8 inject_test_case.py 102 3

测试用例对应关系：
    用例6  → uid102  （答对45个KC，预期kc81排第一）
    用例7  → uid110  （答对89个KC，推荐最多样）
    用例8  → uid197  （答对97个KC，kc20独占Top9）
    用例9  → uid501  （答对27个KC，kc14+kc15排第一）
    用例10 → uid503  （答对30个KC，kc13排第一）


  # 注入用例6（uid102，预期kc81排第一）
  python -X utf8 inject_test_case.py 102 3

  # 注入用例7（uid110，推荐最多样）
  python -X utf8 inject_test_case.py 110 3

  # 注入用例8（uid197，kc20独占Top9）
  python -X utf8 inject_test_case.py 197 3

  # 注入用例9（uid501，kc14+kc15排第一）
  python -X utf8 inject_test_case.py 501 3

  # 注入用例10（uid503，kc13排第一）
  python -X utf8 inject_test_case.py 503 3


"""

import sys
import pymysql

# ── 数据文件路径 ──────────────────────────────────────────────
BASE = "C:/Users/ASUS/important_files/py_local/recommend_system/data/algebra2005"
TEST_TRIPLES = f"{BASE}/test_triples.txt"
Q_MATRIX     = f"{BASE}/Q.txt"

# ── MySQL 连接 ────────────────────────────────────────────────
DB = dict(host="192.168.50.129", user="root", password="123456",
          db="reco_sys", charset="utf8mb4")

TOTAL_EXERCISES = 1084   # Algebra2005 总习题数，用作 total_exercises_done


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)

    py_uid        = int(sys.argv[1])
    mysql_user_id = int(sys.argv[2])
    uid_str       = f"uid{py_uid}"

    print(f"[1] 读取 test_triples.txt → {uid_str} 的 mlkc / pkc / exfr ...")
    mlkc = {}   # kc_index(int) → mastery(float)   0~1
    pkc  = {}   # kc_index(int) → probability(float) 0~1
    exfr = {}   # ex_index(int) → forget_rate(float)  0~1

    with open(TEST_TRIPLES, encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split("\t")
            if len(parts) != 3:
                continue
            entity, value, uid = parts
            if uid != uid_str:
                continue

            if value.startswith("mlkc"):
                kc_idx = int(entity[2:])          # "kc75" → 75
                mlkc[kc_idx] = round(float(value[4:]), 4)
            elif value.startswith("pkc"):
                kc_idx = int(entity[2:])
                pkc[kc_idx] = round(float(value[3:]), 4)
            elif value.startswith("exfr"):
                ex_idx = int(entity[2:])          # "ex1063" → 1063
                exfr[ex_idx] = round(float(value[4:]), 4)

    if not mlkc:
        print(f"  ERROR: test_triples.txt 中找不到 {uid_str}，请检查 uid 是否正确")
        sys.exit(1)

    print(f"  mlkc 条数: {len(mlkc)}")
    print(f"  pkc  条数: {len(pkc)}")
    print(f"  exfr 条数: {len(exfr)}")

    # 打印关键KC（掌握度最低的5个，看pkc是否为0）
    key_kcs = sorted(mlkc.items(), key=lambda x: x[1])[:8]
    print("  掌握度最低的KC:")
    for kc_idx, m in key_kcs:
        p = pkc.get(kc_idx, 0.0)
        print(f"    kc{kc_idx}: mlkc={m:.0%}  pkc={p:.0%}")

    print(f"\n[2] 加载 Q 矩阵 (1084 exercises × 112 KCs) ...")
    Q = []   # Q[ex_idx] = list of covered kc_indices
    with open(Q_MATRIX, encoding="utf-8") as f:
        for row in f:
            cols   = [int(x) for x in row.strip().split(",")]
            covered = [i for i, v in enumerate(cols) if v == 1]
            Q.append(covered)
    print(f"  加载完成：{len(Q)} 道习题")

    print(f"\n[3] 注入 MySQL (user_id={mysql_user_id}) ...")
    conn = pymysql.connect(**DB)
    cur  = conn.cursor()

    # 获取课程（取第一个）
    cur.execute("SELECT id FROM course LIMIT 1")
    row = cur.fetchone()
    if not row:
        print("  ERROR: MySQL 中没有课程，请先创建课程")
        conn.close(); sys.exit(1)

    # 清除旧数据
    cur.execute("DELETE FROM user_kc_state        WHERE user_id = %s", (mysql_user_id,))
    cur.execute("DELETE FROM answer_record         WHERE user_id = %s", (mysql_user_id,))
    cur.execute("DELETE FROM recommendation_record WHERE user_id = %s", (mysql_user_id,))
    print("  已清除旧数据")

    # ── 插入 user_kc_state ────────────────────────────────────
    # kp_id = py_kc_index + 1（py 0-based → MySQL 1-based）
    cur.execute("SELECT id, py_kc_index FROM knowledge_point WHERE py_kc_index IS NOT NULL")
    kp_map = {row[1]: row[0] for row in cur.fetchall()}   # py_kc_index → kp_id

    # total_count 由 pkc 反推：pkc = total_count / TOTAL_EXERCISES
    # correct_count = mastery * total_count（近似）
    inserted_kc = 0
    for kc_idx, mastery in mlkc.items():
        kp_id = kp_map.get(kc_idx)
        if kp_id is None:
            continue
        pkc_val       = pkc.get(kc_idx, 0.0)
        total_count   = round(pkc_val * TOTAL_EXERCISES)
        correct_count = round(mastery * total_count) if total_count > 0 else 0
        cur.execute("""
            INSERT INTO user_kc_state
              (user_id, kp_id, mastery_level, total_count, correct_count)
            VALUES (%s, %s, %s, %s, %s)
        """, (mysql_user_id, kp_id, mastery, total_count, correct_count))
        inserted_kc += 1
    print(f"  插入 user_kc_state: {inserted_kc} 条")

    # ── 插入 answer_record ────────────────────────────────────
    # 目的：让 answerRecordRepository.countDistinctExerciseIdsByUserId() = TOTAL_EXERCISES
    # 同时，利用 exfr 值还原答题结果：exfr=0.7 → 答错(score=0)，其余 → 答对(score=100)
    cur.execute("SELECT id, py_ex_index FROM exercise WHERE py_ex_index IS NOT NULL")
    ex_map = {row[1]: row[0] for row in cur.fetchall()}   # py_ex_index → exercise_id

    # exfr 时间换算：exfr = min(1, hours/720)
    # 答对的题：submitted_at = NOW() - hours，hours = exfr * 720
    # 答错的题：exfr=0.7（Java 固定值），submitted_at 任意，score=0
    inserted_ex = 0
    for ex_idx in range(TOTAL_EXERCISES):
        ex_id = ex_map.get(ex_idx)
        if ex_id is None:
            continue
        fr = exfr.get(ex_idx, 0.0)
        if fr >= 0.65:
            # 答错的题
            score = 0
            time_sql = "NOW()"
        else:
            # 答对的题，按 exfr 反推答题时间
            score    = 100
            hours    = fr * 720
            time_sql = f"DATE_SUB(NOW(), INTERVAL {round(hours)} HOUR)"
        cur.execute(f"""
            INSERT INTO answer_record
              (user_id, exercise_id, answer, status, score, submitted_at)
            VALUES (%s, %s, '[注入]', 'GRADED', %s, {time_sql})
        """, (mysql_user_id, ex_id, score))
        inserted_ex += 1

    print(f"  插入 answer_record: {inserted_ex} 条")
    conn.commit()
    conn.close()

    print()
    print("=" * 60)
    print(f"注入完成！{uid_str} → MySQL user_id={mysql_user_id}")
    print()
    print("现在用该账号登录，在推荐页点击「获取推荐」即可复现测试结果。")
    print("=" * 60)


if __name__ == "__main__":
    main()
