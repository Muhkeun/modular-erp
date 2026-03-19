import type { ReactNode } from "react";
import { clsx } from "clsx";

interface FormFieldProps {
  label: string;
  required?: boolean;
  error?: string;
  helpText?: string;
  children: ReactNode;
  className?: string;
}

export default function FormField({ label, required, error, helpText, children, className }: FormFieldProps) {
  return (
    <div className={clsx("space-y-2", className)}>
      <label className="field-label">
        {label}
        {required && <span className="ml-1 text-red-500">*</span>}
      </label>
      {children}
      {error && (
        <p className="text-xs font-medium text-red-500">{error}</p>
      )}
      {!error && helpText && (
        <p className="field-helper">{helpText}</p>
      )}
    </div>
  );
}
