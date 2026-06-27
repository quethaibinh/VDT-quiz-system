// Quan ly dong ho thoi gian va tinh toan chenh lech thoi gian giua client va server (Time Drift)
export class RuntimeClock {
  private driftMs = 0;

  constructor(serverTimeIso: string) {
    const serverTime = new Date(serverTimeIso).getTime();
    const clientTime = Date.now();
    // Tinh toan thoi gian drift
    this.driftMs = clientTime - serverTime;
  }

  // Uoc luong thoi gian hien tai cua server
  getServerNow(): number {
    return Date.now() - this.driftMs;
  }

  // Tinh so giay con lai truoc khi den thoi han (deadline)
  getRemainingSeconds(deadlineIso: string): number {
    const deadline = new Date(deadlineIso).getTime();
    const serverNow = this.getServerNow();
    return Math.max(0, Math.ceil((deadline - serverNow) / 1000));
  }
}
