import { Button } from "@/components/ui/button";

export type MonitorMobileTab = "PARTICIPANTS" | "EVENTS";

export function MonitorMobileTabs({
  value,
  onChange,
}: {
  value: MonitorMobileTab;
  onChange: (tab: MonitorMobileTab) => void;
}) {
  return (
    <div className="grid grid-cols-2 gap-2 md:hidden">
      <Button
        type="button"
        variant={value === "PARTICIPANTS" ? "primary" : "secondary"}
        onClick={() => onChange("PARTICIPANTS")}
      >
        Học sinh
      </Button>
      <Button
        type="button"
        variant={value === "EVENTS" ? "primary" : "secondary"}
        onClick={() => onChange("EVENTS")}
      >
        Sự kiện
      </Button>
    </div>
  );
}
