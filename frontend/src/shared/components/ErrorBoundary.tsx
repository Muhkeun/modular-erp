import { Component, type ReactNode } from "react";
import { AlertOctagon, ChevronDown, ChevronUp, RotateCcw } from "lucide-react";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  showDetails: boolean;
}

export default class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null, showDetails: false };
  }

  static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error("[ErrorBoundary]", error, info.componentStack);
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null, showDetails: false });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;

      return (
        <div className="flex items-center justify-center min-h-[400px] p-8">
          <div className="w-full max-w-md text-center">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50">
              <AlertOctagon size={24} className="text-red-500" />
            </div>
            <h2 className="mt-4 text-lg font-semibold text-slate-900">
              Something went wrong
            </h2>
            <p className="mt-2 text-sm text-slate-500">
              An unexpected error occurred. Please try again or contact support.
            </p>
            <button
              className="btn-primary mt-6"
              onClick={this.handleRetry}
            >
              <RotateCcw size={16} />
              Retry
            </button>

            {this.state.error && (
              <div className="mt-6 text-left">
                <button
                  className="flex items-center gap-2 text-xs text-slate-400 hover:text-slate-600 transition"
                  onClick={() => this.setState((s) => ({ showDetails: !s.showDetails }))}
                >
                  {this.state.showDetails ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                  Error details
                </button>
                {this.state.showDetails && (
                  <pre className="mt-2 max-h-40 overflow-auto rounded-2xl bg-slate-50 p-4 text-xs text-red-600">
                    {this.state.error.message}
                    {"\n\n"}
                    {this.state.error.stack}
                  </pre>
                )}
              </div>
            )}
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
