package group1.com.MangaSystemAndManagement.model;

/**
 * Technical Spec v2.1 §3.1 — Production Plan status flow.
 *
 * <ul>
 *   <li>{@link #DRAFT}     : Plan created ahead of time for a future cycle.</li>
 *   <li>{@link #ACTIVE}    : Plan is in its official operating window.</li>
 *   <li>{@link #OVERDUE}   : End date passed without Complete or Extend (auto-detected).</li>
 *   <li>{@link #EXTENDED}  : Tantou manually extended the end date.</li>
 *   <li>{@link #COMPLETED} : Tantou closed the plan after 100% chapters DONE.</li>
 * </ul>
 *
 * <p>Decision Log 2026-07-28: {@code PAUSED} and {@code CANCELLED} from BA V3 §2.2 are
 * retired in favour of the v2.1 spec. Plans follow the 5-state fixed cadence above.
 */
public enum PlanStatus {
    DRAFT,
    ACTIVE,
    OVERDUE,
    EXTENDED,
    COMPLETED
}
